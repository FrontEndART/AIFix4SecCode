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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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
    private static final String SUPPORTED_PROBLEM_TYPES_PATH_KEY = "supported_problem_types_path";
    private static final String J2CP_PATH_KEY = "j2cp_path";
    private static final String J2CP_EDITION_KEY = "j2cp_edition";
    private static final String RESULTS_PATH_KEY = "results_path";
    private static final String DESCRIPTION_PATH_KEY = "description_path";
    private static final String PATCH_SAVE_PATH_KEY = "patch_save_path";
    private static final String ARCHIVE_PATH = "archive_path";
    private static final String ARCHIVE_ENABLED = "archive_enabled";
    private String projectName = "";
    private String projectPath = "";
    private String osaPath = "";
    private String osaEdition = "";
    private String supportedProblemTypesPath = "";
    private String j2cpPath = "";
    private String j2cpEdition = "";
    private String resultsPath = "";
    private String descriptionPath = "";
    private String patchSavePath = "";
    private String archivePath = "";
    private boolean archiveEnabled = false;

    public static void main(String[] args) {
        VulnRepairDriver driver = new VulnRepairDriver();
        if (args.length == 1) {
            driver.bootstrap(args[0]);
        } else {
            LOG.error("Unable to process arguments.");
        }
    }

    public void bootstrap(String configPath) {
        LOG.info("Start!");

        Properties properties = new Properties();
        try (InputStream stream = new FileInputStream(configPath)) {
            LOG.info("Attempting to load data from config.properties.");
            properties.load(stream);
            projectName = (String) properties.get(PROJECT_NAME_KEY);
            projectPath = (String) properties.get(PROJECT_PATH_KEY);
            osaPath = properties.get(OSA_PATH_KEY) + File.separator + "Java";
            osaEdition = (String) properties.get(OSA_EDITION_KEY);
            supportedProblemTypesPath = (String) properties.get(SUPPORTED_PROBLEM_TYPES_PATH_KEY);
            j2cpPath = osaPath + File.separator + "WindowsTools";
            j2cpEdition = "JAN2ChangePath";
            resultsPath = (String) properties.get(RESULTS_PATH_KEY);
            archivePath = (String) properties.get(ARCHIVE_PATH);
            archiveEnabled = Boolean.valueOf(properties.get(ARCHIVE_ENABLED).toString());
            descriptionPath = resultsPath + File.separator + "osa_xml";
            patchSavePath = resultsPath + File.separator + "patches";
            LOG.info("Successfully loaded data.");
        } catch (IOException e) {
            LOG.info("Could not find config.properties. Exiting.");
            System.exit(-1);
        }
        try {
            Files.createDirectory(Paths.get(resultsPath));
        } catch (IOException e) {
            LOG.info("Unable to create results folder.");
        }

        String currentTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        SourceCodeCollector scc = new LocalSourceFolder(projectPath);
        scc.collectSourceCode();

        CodeAnalyzer osa = new OpenStaticAnalyzer(osaPath, osaEdition, supportedProblemTypesPath, j2cpPath, j2cpEdition, resultsPath, projectName, patchSavePath);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation());
        codeModels.stream().forEach(cm -> LOG.debug(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = new OpenStaticAnalyzer(osaPath, osaEdition, supportedProblemTypesPath, j2cpPath, j2cpEdition, resultsPath, projectName, patchSavePath);
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation());

        VulnerabilityRepairer vr = new ASGTransformRepair(projectName, projectPath, resultsPath, descriptionPath, patchSavePath);
        int patchCounter1 = 1;
        int patchCounter2 = 1;
        Map<String, Integer> problemTypeCounter = new HashMap<>();
        JSONObject vsCodeConfig = new JSONObject();
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Patch<String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels, patchCounter1++);
            LOG.debug(String.valueOf(patches));
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Patch<String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(
                    osaPath,
                    osaEdition,
                    supportedProblemTypesPath,
                    j2cpPath,
                    j2cpEdition,
                    resultsPath,
                    projectName,
                    patchSavePath
            );
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
            LOG.debug(String.valueOf(candidatePatches));
            if (candidatePatches.isEmpty()) {
                continue;
            }
            JSONArray patchesArray = new JSONArray();
            for (int i = 0; i < candidatePatches.size(); i++) {
                File path = candidatePatches.get(i).getA();
                Patch<String> patch = candidatePatches.get(i).getB();

                // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
                String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}", patchCounter2++, ve.getType(), ve.getStartLine(), ve.getEndLine(), ve.getStartCol(), ve.getEndCol());
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
                        if (lineParts[1].charAt(0) =='\\' || lineParts[1].charAt(0) =='/') {
                            lineParts[1] = lineParts[1].substring(1);
                        }

                        unifiedDiff.set(j, lineParts[0] + lineParts[1]);
                    }

                    String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                    patchWriter.write(diffString);

                    JSONObject patchObject = new JSONObject();
                    patchObject.put("path", patchName);
                    patchObject.put("explanation", "Fix");
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
            textRangeObject.put("startColumn", ve.getStartCol());
            textRangeObject.put("endColumn", ve.getEndCol());

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
                problemTypeCount = "_" + n;
            }

            vsCodeConfig.put(ve.getType() + problemTypeCount, issueObject);
        }

        if (archiveEnabled){
            File src = new File (patchSavePath);
            File dest = new File(archivePath);
            File desc = new File(descriptionPath + File.separator + "description.xml");

            try {
                if(!dest.exists()){
                    Path path = Paths.get(dest.toString());
                    Files.createDirectory(path);
                }
                FileOutputStream fos = new FileOutputStream(dest + File.separator + currentTime + ".zip");
                ZipOutputStream zos = new ZipOutputStream(fos);

                File[] files = src.listFiles();
                for (File fileToZip : files) {
                    FileInputStream fis = new FileInputStream(fileToZip);
                    ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] bytes = new byte[1024];
                    int length;
                    while ((length = fis.read(bytes)) >= 0) {
                        zos.write(bytes, 0, length);
                    }

                    zos.closeEntry();
                    fis.close();
                }
                FileInputStream fis = new FileInputStream(desc);
                ZipEntry zipEntry = new ZipEntry(desc.getName());
                zos.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }

                zos.closeEntry();
                fis.close();

                zos.close();
                fos.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath, "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }

        Utils.deletePatches(patchSavePath, patchCounter1);
    }
}
