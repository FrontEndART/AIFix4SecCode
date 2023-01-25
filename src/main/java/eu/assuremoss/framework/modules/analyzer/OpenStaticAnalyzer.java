package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.*;
import eu.assuremoss.utils.factories.PatchCompilerFactory;
import eu.assuremoss.utils.factories.VulnEntryFactory;
import eu.assuremoss.utils.parsers.SpotBugsParser;
import eu.assuremoss.utils.tools.SourceCompiler;
import lombok.AllArgsConstructor;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.PROJECT_BUILD_TOOL_KEY;
import static eu.assuremoss.utils.Configuration.PROJECT_SOURCE_PATH_KEY;

@AllArgsConstructor
public class OpenStaticAnalyzer implements CodeAnalyzer, VulnerabilityDetector, PatchValidator {
    private static final Logger LOG = LogManager.getLogger(OpenStaticAnalyzer.class);

    private final String osaPath;
    private final String osaEdition;
    private final String j2cpPath;
    private final String j2cpEdition;
    private final String resultsPath;
    private final String validation_results_path;
    private final String projectName;
    private final Map<String, String> supportedProblemTypes;
    private final PathHandler path;

    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation, boolean isValidation) {
        String workingDir = isValidation ? validation_results_path : resultsPath;
        /*PatchCompiler patchCompiler = PatchCompilerFactory.getPatchCompiler(VulnRepairDriver.properties.getProperty(PROJECT_BUILD_TOOL_KEY));
        patchCompiler.compile(srcLocation, Configuration.isTestingEnabled(), true);*/
/*


        String fbFileListPath = String.valueOf(Paths.get(workingDir, "fb_file_list.txt"));
        try (FileWriter fw = new FileWriter(fbFileListPath)) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), patchCompiler.getBuildDirectoryName())));
        } catch (IOException e) {
            LOG.error(e);
        }*/
        SourceCompiler compiler = new SourceCompiler(VulnRepairDriver.properties, isValidation);
        compiler.compile (srcLocation,  Configuration.isTestingEnabled(VulnRepairDriver.properties), isValidation);
        String fbFileListPath = (isValidation?String.valueOf(Paths.get(VulnRepairDriver.properties.getProperty("config.validation_results_path"), "fb_file_list.txt")):String.valueOf(Paths.get(workingDir, "fb_file_list.txt")));
        try (FileWriter fw = new FileWriter(fbFileListPath)) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), compiler.getLastCompiled())));
        } catch (IOException e) {
            LOG.error(e);
        }
        String spotBugsXml = (isValidation?
                String.valueOf(Paths.get(VulnRepairDriver.properties.getProperty("config.validation_results_path"), "spotbugs.xml")):
                String.valueOf(Paths.get(VulnRepairDriver.properties.getProperty("config.results_path"), "spotbugs.xml")));
        compiler.analyze();

        List<CodeModel> resList = new ArrayList<>();

        String[] command = new String[] {
                new File(osaPath, osaEdition + "Java" + Utils.getExtension()).getAbsolutePath(),
                "-resultsDir=" + workingDir,
                "-projectName=" + projectName,
                "-projectBaseDir=" + srcLocation,
                "-cleanResults=0",
                "-currentDate=0",
                "-FBFileList=" + fbFileListPath,
                "-runFB=true",
                "-runPMD=false",
                "-runMET=false",
                "-runUDM=false",
                "-runDCF=false",
                "-runMetricHunter=false",
                "-runLIM2Patterns=false",
                /*"-runChangeTracker=true",
                "-changePathToRelative",*/
                "-FBOptions=-auxclasspath " + Paths.get(srcLocation.getAbsolutePath(), compiler.getLastCompiled(), "dependency")
        };
        //System.out.println(Arrays.toString(command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

        String asgPath = String.valueOf(Paths.get(workingDir,
                projectName,
                "java",
                "0",
                osaEdition.toLowerCase(Locale.ROOT),
                "asg",
                projectName + ".ljsi"));
        String graphXMLPath = String.valueOf(Paths.get(workingDir, projectName, "java", "0", projectName + ".xml"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asgPath)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.OSA_GRAPH_XML, new File(graphXMLPath)));

        String findBugsXMLPath = String.valueOf(Paths.get(workingDir, projectName, "java", "0", osaEdition.toLowerCase(Locale.ROOT), "temp", projectName + "-FindBugs.xml"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.FINDBUGS_XML, new File(findBugsXMLPath)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.SPOTBUGS_XML, new File(spotBugsXml)));

        command = new String[] {
                new File(j2cpPath, j2cpEdition + Utils.getExtension()).getAbsolutePath(),
                asgPath,
                "-from:" + Paths.get(srcLocation.getAbsolutePath(), VulnRepairDriver.properties.getProperty(PROJECT_SOURCE_PATH_KEY)) + File.separator,
                "-to:"
        };
        processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

        return resList;
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        List<CodeModel> resList = new ArrayList<>();
        String spotBugsXml = String.valueOf(Paths.get(VulnRepairDriver.properties.getProperty("config.validation_results_path"), "spotbugs.xml"));
        //String findBugsXMLPath = String.valueOf(Paths.get(validation_results_path, projectName, "java", "0", osaEdition.toLowerCase(Locale.ROOT), "temp", projectName + "-FindBugs.xml"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.SPOTBUGS_XML, new File(spotBugsXml)));
        String graphXMLPath = String.valueOf(Paths.get(validation_results_path, projectName, "java", "0", projectName + ".xml"));
        String asgPath = String.valueOf(Paths.get(validation_results_path,
                projectName,
                "java",
                "0",
                osaEdition.toLowerCase(Locale.ROOT),
                "asg",
                projectName + ".ljsi"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asgPath)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.OSA_GRAPH_XML, new File(graphXMLPath)));
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(analyzeSourceCode(srcLocation, true)); //only for computing the patched asg
        SpotBugsParser sparser = new SpotBugsParser(path, VulnRepairDriver.getConfig().properties, new File(asgPath));


        String findBugsXMLPath = String.valueOf(Paths.get(validation_results_path, projectName, "java", "0", osaEdition.toLowerCase(Locale.ROOT), "temp", projectName + "-FindBugs.xml"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.FINDBUGS_XML, new File(findBugsXMLPath)));
        try {
            FileUtils.copyFile(new File(findBugsXMLPath), new File(spotBugsXml));
        } catch (IOException e) {
            e.printStackTrace();
        }
        /*List<VulnerabilityEntry> */vulnerabilities = null;
        try {
            vulnerabilities = sparser.readXML(true, false);
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
        if (vulnerabilities == null) {
            vulnerabilities = new ArrayList<>();
        }
        return !vulnerabilities.contains(ve);
    }

    @Override
    public List<VulnerabilityEntry> getVulnerabilityLocations(List<CodeModel> analysisResults) {
        List<VulnerabilityEntry> result = new ArrayList<>();

        try {
            Optional<CodeModel> graphXML = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.OSA_GRAPH_XML);
            Optional<CodeModel> findBugsXML = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.FINDBUGS_XML);
            Optional<CodeModel> asg = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.ASG);
            Optional<CodeModel> spotbugs = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.SPOTBUGS_XML);

            VulnEntryFactory vulnEntryFactory = new VulnEntryFactory(findBugsXML, asg);
            result = filteredVulnerabilities(Utils.getNodeList(graphXML, "attribute"))
                    .map(node -> vulnEntryFactory.getVulnEntry(node))
                    .collect(Collectors.toList());

        } catch (DataFormatException e) {
            LOG.error(e);
        }

        return result;
    }

    private Optional<CodeModel> getCodeModel(List<CodeModel> analysisResults, CodeModel.MODEL_TYPES CodeModelType) throws DataFormatException {
        Optional<CodeModel> codeModel = analysisResults.stream().filter(cm -> cm.getType() == CodeModelType).findFirst();
        if (codeModel.isEmpty()) throw new DataFormatException("Could not locate " + CodeModelType + " analysis results, no vulnerabilities were retrieved.");
        return codeModel;
    }

    private Stream<Node> filteredVulnerabilities(NodeList nodeList) {
        return Utils.nodeListToArrayList(nodeList).stream()
                .filter(node -> getNodeAttribute(node, "context").equals("warning")
                                && supportedProblemTypes.get(getNodeAttribute(node, "name")) != null);
    }

    private String getNodeAttribute(Node node, String key) {
        return node.getAttributes().getNamedItem(key).getNodeValue();
    }

}
