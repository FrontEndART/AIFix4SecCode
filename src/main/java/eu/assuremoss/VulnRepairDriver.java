package eu.assuremoss;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.*;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.repair.ASGTransformRepair;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import eu.assuremoss.utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The main driver class that runs the vulnerability repair workflow
 *
 */
public class VulnRepairDriver
{
    private static final Logger logger = LogManager.getLogger(VulnRepairDriver.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PROJECT_NAME_KEY = "project_name";
    private static String projectName = "";
    private static final String PROJECT_PATH_KEY = "project_path";
    private static String sourceCodePath = "";
    private static final String OSA_PATH = "osa_path";
    private static String osaPath = "";
    private static final String PATCH_SAVE_PATH_KEY = "patch_save_path";
    private static String patchSavePath = "";

    public static void main( String[] args )
    {
        logger.info("Start!");

        Properties properties = new Properties();
        try {
            logger.info("Attempting to load data from config.properties.");
            properties.load(new BufferedReader(new FileReader(CONFIG_FILE_NAME)));
            projectName = (String) properties.get(PROJECT_NAME_KEY);
            sourceCodePath = (String) properties.get(PROJECT_PATH_KEY);
            osaPath = (String) properties.get(OSA_PATH);
            patchSavePath = (String) properties.get(PATCH_SAVE_PATH_KEY);
            logger.info("Successfully loaded data.");
        } catch (IOException e) {
            try {
                logger.info("Could not find config.properties. Creating file.");
                properties.setProperty(PROJECT_NAME_KEY, "");
                properties.setProperty(PROJECT_PATH_KEY, "");
                properties.setProperty(OSA_PATH, "");
                properties.setProperty(PATCH_SAVE_PATH_KEY, "");
                properties.store(new FileWriter(CONFIG_FILE_NAME), "");
                return;
            } catch (IOException ioException) {
                logger.error("Error during file creation.");
                ioException.printStackTrace();
            }
        }

        SourceCodeCollector scc = new LocalSourceFolder(sourceCodePath);
        scc.collectSourceCode();

        CodeAnalyzer osa = new OpenStaticAnalyzer(osaPath);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation());
        codeModels.stream().forEach(cm -> System.out.println(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = new OpenStaticAnalyzer(osaPath);
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation());

        VulnerabilityRepairer vr = new ASGTransformRepair();
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Patch<String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            System.out.println(patches);
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Patch<String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(osaPath);
            List<Pair<File, Patch<String>>> candidatePatches = new ArrayList<>();
            for (Pair<File, Patch<String>> patch : filteredPatches) {
                comp.applyPatch(patch, scc.getSourceCodeLocation());
                if (pv.validatePatch(scc.getSourceCodeLocation(), ve, patch)) {
                    candidatePatches.add(patch);
                }
                comp.revertPatch(patch, scc.getSourceCodeLocation());
            }
            // TODO: print out patches to files or pass them to a Visualizer
        }
    }

    public static String getProjectName() {
        return projectName;
    }
    public static String getSourceCodePath() {
        return sourceCodePath;
    }
    public static String getOsaPath() {
        return osaPath;
    }
    public static String getPatchSavePath() {
        return patchSavePath;
    }
}
