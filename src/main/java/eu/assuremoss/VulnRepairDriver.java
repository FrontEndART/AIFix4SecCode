package eu.assuremoss;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.assuremoss.framework.api.*;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.*;
import eu.assuremoss.utils.factories.PatchCompilerFactory;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.factories.ToolFactory;
import eu.assuremoss.utils.parsers.SpotBugsParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.*;
import static eu.assuremoss.utils.Utils.getConfigFile;
import static eu.assuremoss.utils.Utils.getMappingFile;


/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private MLogger MLOG;
    private static final Logger LOG = LogManager.getLogger(VulnRepairDriver.class);
    public static Properties properties;
    private final PatchCompiler patchCompiler;
    private final PathHandler path;
    private final Statistics statistics;
    private int patchCounter = 1;
    private static Configuration config;

    private void setConfig(Configuration config) {
        this.config = config;
    }
    public static Configuration getConfig(){return config;}

    public static void main(String[] args) throws IOException {
        Configuration config = new Configuration(getConfigFile(args), getMappingFile(args));
        VulnRepairDriver driver = new VulnRepairDriver(config.properties);
        driver.setConfig(config);
        driver.bootstrap(config.properties);
    }

    public VulnRepairDriver(Properties properties) throws IOException {
        this.patchCompiler = PatchCompilerFactory.getPatchCompiler(properties.getProperty(PROJECT_BUILD_TOOL_KEY));
        this.path = new PathHandler(properties.getProperty(RESULTS_PATH_KEY), properties.getProperty(VALIDATION_RESULTS_PATH_KEY));
        this.statistics = new Statistics(path);
        VulnRepairDriver.properties = properties;

        Utils.initResourceFiles(properties, path);
        MLOG = new MLogger("log.txt", path, Configuration.isTestingEnabled(properties));  // TODO get 'log.txt' from pathHandler
    }

    public void bootstrap(Properties props) {
        MLOG.fInfo("Start!");

        // 0. Setup
        Date startTime = new Date();
        String startTimeStr = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(startTime);

        // 1. Get source code
        MLOG.info("Project source acquiring started");
        SourceCodeCollector scc = new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY));
        scc.collectSourceCode();

        // 2. Analyze source code
        MLOG.info("Code analysis started");
        CodeAnalyzer osa = ToolFactory.createOsa(props, path);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation(), false);

        // 2. Produces :- ASG
        String ljsiName="";
        try {
            ljsiName = String.valueOf(Utils.getCodeModel(codeModels, CodeModel.MODEL_TYPES.ASG).get().getModelPath());
        }  catch (DataFormatException e) {
            e.printStackTrace();
        }
        VulnerabilityRepairer vulnRepairer = ToolFactory.createASGTransformRepair(props, ljsiName);

        // 3. Detect vulnerabilities
        VulnerabilityDetector vulnDetector = ToolFactory.createOsa(props, path);

        // 3. Produces :- vulnerability locations
        //List<VulnerabilityEntry> vulnerabilityLocations = vulnDetector.getVulnerabilityLocations(codeModels);
        MLOG.info("Spotbugs analysis started");

        List<VulnerabilityEntry> vulnerabilityLocations = null;
        try {
            SpotBugsParser sparser = new SpotBugsParser(path, config.properties,  Utils.getCodeModel(codeModels, CodeModel.MODEL_TYPES.ASG).get().getModelPath());
            vulnerabilityLocations = sparser.readXML(false, true);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        if (vulnerabilityLocations == null) {
            vulnerabilityLocations = new ArrayList<>();
        }

        MLOG.info(String.format("Detected %d vulnerabilities", vulnerabilityLocations.size()));
        statistics.saveVulnerabilityStatistics(vulnerabilityLocations);

        if (vulnerabilityLocations.size() == 0) {
            MLOG.ninfo("Framework repair finished!");
            return;
        }

        // == Transform code / repair ==
        Map<String, List<JSONObject>> problemFixMap = new HashMap<>();

        int vulnIndex = 0;
        for (VulnerabilityEntry vulnEntry : vulnerabilityLocations) {
            // - Init -
            vulnIndex++;
            //if (vulnIndex>2) return;
            //MLOG.changeOutPutFile(path.vulnBuildLogFile(vulnIndex));
            MLogger buildLogger = new MLogger(path.vulnBuildLogFile(vulnIndex));
            MLogger.setActiveLogger(buildLogger);

            // - Skip if column info was not retrieved -
            if (vulnEntry.getStartCol() == -1 && vulnEntry.getEndCol() == -1) {
                buildLogger.ninfo(String.format("No column info were retrieved, skipping vulnerability %d/%d", vulnIndex, vulnerabilityLocations.size()));
                continue;
            }

            // - Generate repair patches -
            buildLogger.ninfo(String.format("Generating patches for vulnerability %d/%d", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> patches = vulnRepairer.generateRepairPatches(scc.getSourceCodeLocation(), vulnEntry, codeModels);
            vulnEntry.setGeneratedPatches(patches.size());

            //  - Applying & Compiling patches -
            buildLogger.info(String.format("Compiling patches for vulnerability %d/%d", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = patchCompiler.applyAndCompile(scc.getSourceCodeLocation(), patches, Configuration.isTestingEnabled(properties));

            vulnEntry.setFilteredPatches(filteredPatches.size());

            //  - Testing Patches -
            buildLogger.info(String.format("Verifying patches for vulnerability %d/%d", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = getCandidatePatches(props, scc, vulnEntry, patchCompiler, filteredPatches);
            vulnEntry.setVerifiedPatches(candidatePatches.size());

            // - Save patches -
            Utils.createDirectory(patchSavePath(props));
            if (candidatePatches.isEmpty()) {
                buildLogger.info("No patch candidates were found, skipping!");
                continue;
            }

            buildLogger.info(String.format("Writing out candidate patches for vulnerability %d/%d", vulnIndex, vulnerabilityLocations.size()));
            if (!problemFixMap.containsKey(vulnEntry.getType())) {
                problemFixMap.put(vulnEntry.getType(), new ArrayList());
            }
            problemFixMap.get(vulnEntry.getType()).add(generateFixEntity(props, vulnEntry, candidatePatches));
            buildLogger.closeFile();
        }

        MLogger.setActiveLogger(MLOG);

        JSONObject vsCodeConfig = getVSCodeConfig(problemFixMap);

        try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath(props), "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (archiveEnabled(props)) {
            Utils.archiveResults(patchSavePath(props), props.getProperty(ARCHIVE_PATH), descriptionPath(props), startTimeStr);
        }

        Utils.deleteIntermediatePatches(patchSavePath(props));
        Utils.saveElapsedTime(startTime);
        statistics.createResultStatistics(props, vulnerabilityLocations);

        MLOG.info("Framework repair finished!");
        MLOG.closeFile();
    }

    private JSONObject getVSCodeConfig(Map<String, List<JSONObject>> problemFixMap) {
        JSONObject vsCodeConfig = new JSONObject();
        for (String problemType : problemFixMap.keySet()) {
            JSONArray fixesArray = new JSONArray();
            fixesArray.addAll(problemFixMap.get(problemType));
            vsCodeConfig.put(problemType, fixesArray);
        }

        return vsCodeConfig;
    }

    private List<Pair<File, Pair<Patch<String>, String>>> getCandidatePatches(Properties props, SourceCodeCollector scc, VulnerabilityEntry vulnEntry, PatchCompiler comp, List<Pair<File, Pair<Patch<String>, String>>> filteredPatches) {
        List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
        PatchValidator patchValidator = ToolFactory.createOsa(props, path);

        for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : filteredPatches) {
            Patch<String> rawPatch = patchWithExplanation.getB().getA();
            Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);

            comp.applyPatch(patch, scc.getSourceCodeLocation());
            if (patchValidator.validatePatch(scc.getSourceCodeLocation(), vulnEntry, patch)) {
                candidatePatches.add(patchWithExplanation);
            }
            comp.revertPatch(patch, scc.getSourceCodeLocation());
        }

        return candidatePatches;
    }

    private JSONObject generateFixEntity(Properties props, VulnerabilityEntry vulnEntry, List<Pair<File, Pair<Patch<String>, String>>> candidatePatches) {
        JSONArray patchesArray = new JSONArray();
        JSONObject issueObject = new JSONObject();
        for (int i = 0; i < candidatePatches.size(); i++) {
            File path = candidatePatches.get(i).getA();
            Patch<String> patch = candidatePatches.get(i).getB().getA();
            String explanation = candidatePatches.get(i).getB().getB();

            // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
            String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}.diff", patchCounter++, vulnEntry.getType(), vulnEntry.getStartLine(), vulnEntry.getEndLine(), vulnEntry.getStartCol(), vulnEntry.getEndCol());
            try (PrintWriter patchWriter = new PrintWriter(String.valueOf(Paths.get(patchSavePath(props), patchName)))) {
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(path.getPath(), path.getPath(),
                                Arrays.asList(Files.readString(Path.of(path.getAbsolutePath())).split("\n")), patch, 2);

                // make the path in the patch file relative to the project path
                for (int j = 0; j < 2; j++) {
                    String line = unifiedDiff.get(j);

                    String regex = props.getProperty(PROJECT_PATH_KEY);
                    regex = regex.replaceAll("\\\\", "\\\\\\\\");

                    String[] lineParts = line.split(regex);
                    if (lineParts[1].charAt(0) == '\\' || lineParts[1].charAt(0) == '/') {
                        lineParts[1] = lineParts[1].substring(1);
                    }

                    unifiedDiff.set(j, lineParts[0] + lineParts[1]);
                }

                String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                patchWriter.write(diffString);

                JSONObject patchObject = new JSONObject();
                patchObject.put("path", patchName);
                patchObject.put("explanation", explanation);
                patchObject.put("score", 10);
                patchesArray.add(patchObject);
            } catch (IOException e) {
                LOG.error("Failed to save candidate patch: " + patch);
            }
        }

        JSONObject textRangeObject = new JSONObject();
        textRangeObject.put("startLine", vulnEntry.getStartLine());
        textRangeObject.put("endLine", vulnEntry.getEndLine());
        textRangeObject.put("startColumn", vulnEntry.getStartCol() - 1);
        textRangeObject.put("endColumn", vulnEntry.getEndCol() - 1);

        issueObject.put("patches", patchesArray);
        issueObject.put("textRange", textRangeObject);

        return issueObject;
    }


}
