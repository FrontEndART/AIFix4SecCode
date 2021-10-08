package eu.assuremoss;

import com.github.difflib.patch.Patch;
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private static final Logger logger = LogManager.getLogger(VulnRepairDriver.class);

    private static final String CONFIG_FILE_NAME = "config.properties";
    private static final String PROJECT_NAME_KEY = "project_name";
    private static final String PROJECT_PATH_KEY = "project_path";
    private static final String OSA_PATH_KEY = "osa_path";
    private static final String OSA_EDITION_KEY = "osa_edition";
    private static final String OSA_RES_PATH_KEY = "osa_results_dir";
    private static final String PATCH_SAVE_PATH_KEY = "patch_save_path";
    private String projectName = "";
    private String sourceCodePath = "";
    private String osaPath = "";
    private String osaEdition = "";
    private String patchSavePath = "";
    private String resultsDir = "";

    public static void main(String[] args) {
        VulnRepairDriver driver = new VulnRepairDriver();
        driver.bootstrap();
    }

    public void bootstrap() {

        logger.info("Start!");

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Properties properties = new Properties();
        try (InputStream resourceStream = loader.getResourceAsStream(CONFIG_FILE_NAME)) {
            logger.info("Attempting to load data from config.properties.");
            properties.load(resourceStream);
            projectName = (String) properties.get(PROJECT_NAME_KEY);
            sourceCodePath = (String) properties.get(PROJECT_PATH_KEY);
            osaPath = (String) properties.get(OSA_PATH_KEY);
            osaEdition = (String) properties.get(OSA_EDITION_KEY);
            patchSavePath = (String) properties.get(PATCH_SAVE_PATH_KEY);
            resultsDir = (String) properties.get(OSA_RES_PATH_KEY);
            logger.info("Successfully loaded data.");
        } catch (IOException e) {
            logger.info("Could not find config.properties. Exiting.");
            System.exit(-1);
        }

        SourceCodeCollector scc = new LocalSourceFolder(sourceCodePath);
        scc.collectSourceCode();

        CodeAnalyzer osa = new OpenStaticAnalyzer(osaPath, osaEdition, resultsDir, projectName, patchSavePath);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation());
        codeModels.stream().forEach(cm -> logger.debug(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = new OpenStaticAnalyzer(osaPath, osaEdition, resultsDir, projectName, patchSavePath);
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation());

        VulnerabilityRepairer vr = new ASGTransformRepair();
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Patch<String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            System.out.println(patches);
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Patch<String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(osaPath,
                    osaEdition,
                    resultsDir,
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
            // TODO: print out patches to files or pass them to a Visualizer
        }
    }
}
