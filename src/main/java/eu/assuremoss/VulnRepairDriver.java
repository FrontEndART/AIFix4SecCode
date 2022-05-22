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
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.factories.ToolFactory;
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

import static eu.assuremoss.utils.Configuration.*;
import static eu.assuremoss.utils.Utils.getConfigFile;


/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private static final Logger LOG = LogManager.getLogger(VulnRepairDriver.class);
    public static MLogger MLOG;
    private int patchCounter = 1;

    public static void main(String[] args) throws IOException {
        VulnRepairDriver driver = new VulnRepairDriver();
        Configuration config = new Configuration(getConfigFile(args));

        Utils.createDirectoryForResults(config.properties);
        Utils.createDirectoryForValidation(config.properties);
        Utils.createEmptyLogFile(config.properties);

        MLOG = new MLogger(config.properties, "log.txt");

        driver.bootstrap(config.properties);
    }

    public void bootstrap(Properties props) {
        MLOG.fInfo("Start!");

        // 0. Setup
        String currentTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        // 1. Get source code
        MLOG.info("Project source acquiring started");
        SourceCodeCollector scc = new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY));
        scc.collectSourceCode();

        // 2. Analyze source code
        MLOG.info("Code analysis started");
        CodeAnalyzer osa = ToolFactory.createOsa(props);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation(), false);

        // 2. Produces :- ASG
        VulnerabilityRepairer vulnRepairer = ToolFactory.createASGTransformRepair(props);

        // 3. Detect vulnerabilities
        VulnerabilityDetector vulnDetector = ToolFactory.createOsa(props);

        // 3. Produces :- vulnerability locations
        List<VulnerabilityEntry> vulnerabilityLocations = vulnDetector.getVulnerabilityLocations(scc.getSourceCodeLocation(), codeModels);
        MLOG.info(String.format("Detected %d vulnerabilities", vulnerabilityLocations.size()));
        vulnerabilityLocations.forEach(vulnEntry -> MLOG.fInfo(vulnEntry.getType() + " -> " + vulnEntry.getStartLine()));

        // == Transform code / repair ==
        Map<String, List<JSONObject>> problemFixMap = new HashMap<>();

        int vulnIndex = 0;
        for (VulnerabilityEntry vulnEntry : vulnerabilityLocations) {
            // - Init -
            vulnIndex++;
            PatchCompiler comp = new MavenPatchCompiler();

            // - Generate repair patches -
            MLOG.ninfo(String.format("Generating patches for %d/%d vulnerability", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> patches = vulnRepairer.generateRepairPatches(scc.getSourceCodeLocation(), vulnEntry, codeModels);

            //  - Applying & Compiling patches -
            MLOG.info(String.format("Compiling patches for %d/%d vulnerability", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);

            //  - Testing Patches -
            MLOG.info(String.format("Verifying patches for %d/%d vulnerability", vulnIndex, vulnerabilityLocations.size()));
            List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = getCandidatePatches(props, scc, vulnEntry, comp, filteredPatches);

            // - Save patches -
            Utils.createDirectoryForPatches(props);
            if (candidatePatches.isEmpty()) {
                MLOG.info("No patch candidates were found, skipping!");
                continue;
            }

            MLOG.info(String.format("Writing out patch candidates patches for %d/%d vulnerability", vulnIndex, vulnerabilityLocations.size()));
            if (!problemFixMap.containsKey(vulnEntry.getType())) {
                problemFixMap.put(vulnEntry.getType(), new ArrayList());
            }
            problemFixMap.get(vulnEntry.getType()).add(generateFixEntity(props, vulnEntry, candidatePatches));
        }

        JSONObject vsCodeConfig = getVSCodeConfig(problemFixMap);

        try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath(props), "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (archiveEnabled(props)) {
            Utils.archiveResults(patchSavePath(props), props.getProperty(ARCHIVE_PATH), descriptionPath(props), currentTime);
        }

        Utils.deleteIntermediatePatches(patchSavePath(props));
    }

    private JSONObject getVSCodeConfig(Map<String, List<JSONObject>> problemFixMap) {
        JSONObject vsCodeConfig = new JSONObject();
        for(String problemType : problemFixMap.keySet()) {
            JSONArray fixesArray = new JSONArray();
            fixesArray.addAll(problemFixMap.get(problemType));
            vsCodeConfig.put(problemType, fixesArray);
        }

        return vsCodeConfig;
    }

    private List<Pair<File, Pair<Patch<String>, String>>> getCandidatePatches(Properties props, SourceCodeCollector scc, VulnerabilityEntry vulnEntry, PatchCompiler comp, List<Pair<File, Pair<Patch<String>, String>>> filteredPatches) {
        List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
        PatchValidator patchValidator = ToolFactory.createOsa(props);

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
