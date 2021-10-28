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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private static final Logger LOG = LogManager.getLogger(VulnRepairDriver.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PROJECT_NAME_KEY = "project_name";
    private static final String PROJECT_PATH_KEY = "project_path";
    private static final String OSA_PATH_KEY = "osa_path";
    private static final String OSA_EDITION_KEY = "osa_edition";
    private static final String OSA_RESULTS_PATH_KEY = "osa_results_path";
    private static final String OSA_DESCRIPTION_PATH_KEY = "osa_description_path";
    private static final String PATCH_SAVE_PATH_KEY = "patch_save_path";
    private String projectName = "";
    private String projectPath = "";
    private String osaPath = "";
    private String osaEdition = "";
    private String osaResultsPath = "";
    private static String osaDescriptionPath = "";
    private String patchSavePath = "";

    public static void main(String[] args) {
        VulnRepairDriver driver = new VulnRepairDriver();
        driver.bootstrap();
    }

    public void bootstrap() {
        LOG.info("Start!");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream(CONFIG_FILE_NAME)) {
            LOG.info("Attempting to load data from config.properties.");
            properties.load(resourceStream);
            projectName = (String) properties.get(PROJECT_NAME_KEY);
            projectPath = (String) properties.get(PROJECT_PATH_KEY);
            osaPath = (String) properties.get(OSA_PATH_KEY);
            osaEdition = (String) properties.get(OSA_EDITION_KEY);
            osaResultsPath = (String) properties.get(OSA_RESULTS_PATH_KEY);
            osaDescriptionPath = (String) properties.get(OSA_DESCRIPTION_PATH_KEY);
            patchSavePath = (String) properties.get(PATCH_SAVE_PATH_KEY);
            LOG.info("Successfully loaded data.");
        } catch (IOException e) {
            LOG.info("Could not find config.properties. Exiting.");
            System.exit(-1);
        }

        SourceCodeCollector scc = new LocalSourceFolder(projectPath);
        scc.collectSourceCode();

        CodeAnalyzer osa = new OpenStaticAnalyzer(osaPath, osaEdition, osaResultsPath, projectName, patchSavePath);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation());
        codeModels.stream().forEach(cm -> LOG.debug(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = new OpenStaticAnalyzer(osaPath, osaEdition, osaResultsPath, projectName, patchSavePath);
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation());

        VulnerabilityRepairer vr = new ASGTransformRepair();
        vr.generateDescription(new File(osaDescriptionPath), vulnerabilityLocations);

        int patchCounter = 1;
        int jsonCounter = 1;
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Patch<String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            LOG.debug(patches);
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Patch<String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(osaPath,
                    osaEdition,
                    osaResultsPath,
                    projectName,
                    patchSavePath);
            List<Pair<File, Patch<String>>> candidatePatches = new ArrayList<>();
            for (Pair<File, Patch<String>> patch : filteredPatches) {
                comp.applyPatch(patch, scc.getSourceCodeLocation());
                if (pv.validatePatch(scc.getSourceCodeLocation(), ve, patch)) {
                    candidatePatches.add(patch);
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
            JSONArray patchesArray = new JSONArray();
            for (int i = 0; i < candidatePatches.size(); i++) {
                File path = candidatePatches.get(i).getA();
                Patch<String> patch = candidatePatches.get(i).getB();
                // TODO: generate the necessary meta-info json as well with vulnerability/patch candidate mapping (see #7)
                try (PrintWriter patchWriter = new PrintWriter(String.valueOf(Paths.get(patchSavePath, "patch" + patchCounter)))) {
                    List<String> unifiedDiff =
                            UnifiedDiffUtils.generateUnifiedDiff(path.getPath(), path.getPath(),
                                    Arrays.asList(Files.readString(Path.of(path.getAbsolutePath())).split("\n")), patch, 2);
                    String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                    patchWriter.write(diffString);

                    JSONObject patchObject = new JSONObject();
                    patchObject.put("path", Paths.get(patchSavePath, "patch" + patchCounter).toString());
                    patchObject.put("explanation", "");
                    patchObject.put("score", 10);
                    patchesArray.add(patchObject);
                } catch (IOException e) {
                    LOG.error("Failed to save candidate patch: " + patch);
                }
            }
            JSONObject object = new JSONObject();
            JSONObject issueObject = new JSONObject();

            JSONObject textRangeObject = new JSONObject();
            textRangeObject.put("startLine", ve.getStartLine());
            textRangeObject.put("endLine", ve.getEndLine());
            textRangeObject.put("startColumn", ve.getStartCol());
            textRangeObject.put("endColumn", ve.getEndCol());

            issueObject.put("patches", patchesArray);
            issueObject.put("textRange", textRangeObject);

            object.put("Issue", issueObject);

            try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath, "patch" + jsonCounter++ + ".json")))) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                JsonElement element = JsonParser.parseString(object.toJSONString());
                fw.write(gson.toJson(element));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
