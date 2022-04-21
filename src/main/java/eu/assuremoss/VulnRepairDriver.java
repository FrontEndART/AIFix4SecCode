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
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.repair.ASGTransformRepair;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
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

/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private static final Logger LOG = LogManager.getLogger(VulnRepairDriver.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PROJECT_NAME_KEY = "config.project_name";
    private static final String PROJECT_PATH_KEY = "config.project_path";
    private static final String OSA_PATH_KEY = "config.osa_path";
    private static final String OSA_EDITION_KEY = "config.osa_edition";
    private static final String RESULTS_PATH_KEY = "config.results_path";
    private static final String VALIDATION_RESULTS_PATH_KEY = "config.validation_results_path";
    private static final String ARCHIVE_PATH = "config.archive_path";
    private static final String ARCHIVE_ENABLED = "config.archive_enabled";

    private String projectName = "";
    private String projectPath = "";
    private String osaPath = "";
    private String osaEdition = "";
    private String supportedProblemTypesPath = "";
    private String j2cpPath = "";
    private String j2cpEdition = "";
    private String resultsPath = "";
    private String validation_results_path = "";
    private String descriptionPath = "";
    private String patchSavePath = "";
    private String archivePath = "";
    private boolean archiveEnabled = false;

    public static void main(String[] args) {
        VulnRepairDriver driver = new VulnRepairDriver();
        Properties configProps = new Properties();
        if (args.length == 1) {
            try (InputStream stream = new FileInputStream(args[0])) {
                configProps.load(stream);
            } catch (IOException e) {
                LOG.info("Could not load " + args[0] + ". Exiting.");
                System.exit(-1);
            }
        } else {
            LOG.warn("No configuration properties file is provided, using the default: " + CONFIG_FILE_NAME + ".");
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            try (InputStream stream = loader.getResourceAsStream(CONFIG_FILE_NAME)) {
                configProps.load(stream);
            } catch (IOException e) {
                LOG.info("Could not load " + CONFIG_FILE_NAME + ". Exiting.");
                System.exit(-1);
            }
        }

        driver.bootstrap(configProps);
    }

    public void bootstrap(Properties properties) {
        LOG.info("Start!");

        projectName = properties.getProperty(PROJECT_NAME_KEY);
        projectPath = properties.getProperty(PROJECT_PATH_KEY);
        osaPath = String.valueOf(Paths.get(properties.getProperty(OSA_PATH_KEY), "Java"));
        osaEdition = properties.getProperty(OSA_EDITION_KEY);
        j2cpPath = String.valueOf(Paths.get(osaPath, (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ? "WindowsTools" : "LinuxTools")));
        j2cpEdition = "JAN2ChangePath";
        resultsPath = properties.getProperty(RESULTS_PATH_KEY);
        validation_results_path = properties.getProperty(VALIDATION_RESULTS_PATH_KEY);
        archivePath = properties.getProperty(ARCHIVE_PATH);
        archiveEnabled = Boolean.parseBoolean(properties.getProperty(ARCHIVE_ENABLED));
        descriptionPath = String.valueOf(Paths.get(resultsPath, "osa_xml"));
        patchSavePath = String.valueOf(Paths.get(resultsPath, "patches"));

        LOG.info("Successfully loaded configuration properties.");

        try {
            Files.createDirectory(Paths.get(resultsPath));
        } catch (IOException e) {
            LOG.info("Unable to create results folder.");
        }

        String currentTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        SourceCodeCollector scc = new LocalSourceFolder(projectPath);
        scc.collectSourceCode();

        CodeAnalyzer osa = new OpenStaticAnalyzer(osaPath, osaEdition, j2cpPath, j2cpEdition, resultsPath, validation_results_path, projectName, Utils.getWarningMappingFromProp(properties));
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation(), false);
        codeModels.stream().forEach(cm -> LOG.debug(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = new OpenStaticAnalyzer(osaPath, osaEdition, j2cpPath, j2cpEdition, resultsPath, validation_results_path, projectName, Utils.getWarningMappingFromProp(properties));
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation(), codeModels);
        vulnerabilityLocations.forEach(ve -> System.out.println(ve.getType() + " -> " + ve.getStartLine()));

        VulnerabilityRepairer vr = new ASGTransformRepair(projectName, projectPath, resultsPath, descriptionPath, patchSavePath);
        //int patchCounter1 = 1;
        int patchCounter2 = 1;
        Map<String, Integer> problemTypeCounter = new HashMap<>();
        JSONObject vsCodeConfig = new JSONObject();

        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Pair<Patch<String>, String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            LOG.debug(String.valueOf(patches));
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(
                    osaPath,
                    osaEdition,
                    j2cpPath,
                    j2cpEdition,
                    resultsPath,
                    validation_results_path,
                    projectName,
                    Utils.getWarningMappingFromProp(properties)
            );
            List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
            for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : filteredPatches) {
                Patch<String> rawPatch = patchWithExplanation.getB().getA();
                Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);
                comp.applyPatch(patch, scc.getSourceCodeLocation());
                if (pv.validatePatch(scc.getSourceCodeLocation(), ve, patch)) {
                    candidatePatches.add(patchWithExplanation);
                }
                comp.revertPatch(patch, scc.getSourceCodeLocation());
            }
            File patchSavePathDir = new File(patchSavePath);
            if (!patchSavePathDir.exists()) {
                try {
                    Files.createDirectory(Paths.get(patchSavePath));
                } catch (IOException e) {
                    LOG.error("Failed to create directory for patches.");
                }
            }
            LOG.debug(String.valueOf(candidatePatches));
            if (candidatePatches.isEmpty()) {
                continue;
            }
            JSONArray patchesArray = new JSONArray();
            for (int i = 0; i < candidatePatches.size(); i++) {
                File path = candidatePatches.get(i).getA();
                Patch<String> patch = candidatePatches.get(i).getB().getA();
                String explanation = candidatePatches.get(i).getB().getB();

                // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
                String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}.diff", patchCounter2++, ve.getType(), ve.getStartLine(), ve.getEndLine(), ve.getStartCol(), ve.getEndCol());
                try (PrintWriter patchWriter = new PrintWriter(String.valueOf(Paths.get(patchSavePath, patchName)))) {
                    List<String> unifiedDiff =
                            UnifiedDiffUtils.generateUnifiedDiff(path.getPath(), path.getPath(),
                                    Arrays.asList(Files.readString(Path.of(path.getAbsolutePath())).split("\n")), patch, 2);

                    // make the path in the patch file relative to the project path
                    for (int j = 0; j < 2; j++) {
                        String line = unifiedDiff.get(j);

                        String regex = projectPath;
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
            JSONObject issueObject = new JSONObject();

            JSONObject textRangeObject = new JSONObject();
            textRangeObject.put("startLine", ve.getStartLine());
            textRangeObject.put("endLine", ve.getEndLine());
            textRangeObject.put("startColumn", ve.getStartCol() - 1);
            textRangeObject.put("endColumn", ve.getEndCol() - 1);

            issueObject.put("patches", patchesArray);
            issueObject.put("textRange", textRangeObject);

            String problemTypeCount;
            if (problemTypeCounter.get(ve.getType()) == null) {
                problemTypeCounter.put(ve.getType(), 1);
                problemTypeCount = "";
            } else {
                int n = problemTypeCounter.get(ve.getType());
                n++;
                problemTypeCounter.put(ve.getType(), n);
                problemTypeCount = "#" + n;
            }

            vsCodeConfig.put(ve.getType() + problemTypeCount, issueObject);
        }

       try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath, "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (archiveEnabled) {
            Utils.archiveResults(patchSavePath, archivePath, descriptionPath, currentTime);
        }

        Utils.deleteIntermediatePatches(patchSavePath);
    }
}
