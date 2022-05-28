package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.factories.PatchCompilerFactory;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.factories.VulnEntryFactory;
import lombok.AllArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.PROJECT_BUILD_TOOL_KEY;

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

    public static final HashMap<String, String> vulnMap = new HashMap<>() {{
        // TODO: read from config file
        put("FB_EiER", "EI_EXPOSE_REP2");
        put("FB_EER", "EI_EXPOSE_REP2");
        put("FB_NNPD", "NP_NULL_PARAM_DEREF");
        put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        put("FB_MSBF", "MS_SHOULD_BE_FINAL");
    }};

    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation, boolean isValidation) {
        PatchCompiler patchCompiler = PatchCompilerFactory.getPatchCompiler(VulnRepairDriver.properties.getProperty(PROJECT_BUILD_TOOL_KEY));
        patchCompiler.compile(srcLocation, true, true);

        String workingDir = isValidation ? validation_results_path : resultsPath;

        String fbFileListPath = String.valueOf(Paths.get(workingDir, "fb_file_list.txt"));
        try (FileWriter fw = new FileWriter(fbFileListPath)) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), patchCompiler.getBuildDirectoryName())));
        } catch (IOException e) {
            LOG.error(e);
        }

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
                "-FBOptions=-auxclasspath " + Paths.get(srcLocation.getAbsolutePath(), patchCompiler.getBuildDirectoryName(), "dependency")
        };
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

        String findBugsXMLPath = String.valueOf(Paths.get(workingDir, projectName, "java", "0", "openstaticanalyzer", "temp", projectName + "-FindBugs.xml"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.FINDBUGS_XML, new File(findBugsXMLPath)));

        command = new String[] {
                new File(j2cpPath, j2cpEdition + Utils.getExtension()).getAbsolutePath(),
                asgPath,
                "-from:" + Paths.get(srcLocation.getAbsolutePath(), "src", "main", "java") + File.separator,
                "-to:"
        };
        processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

        return resList;
    }


    private String getNodeAttribute(Node node, String key) {
        return node.getAttributes().getNamedItem(key).getNodeValue();
    }

    private List<Node> nodeListToArrayList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .collect(Collectors.toList());
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation,
                analyzeSourceCode(srcLocation, true));
        return !vulnerabilities.contains(ve);
    }

    @Override
    public List<VulnerabilityEntry> getVulnerabilityLocations(File srcLocation, List<CodeModel> analysisResults) {
        List<VulnerabilityEntry> result = new ArrayList<>();

        try {
            Optional<CodeModel> graphXML = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.OSA_GRAPH_XML);
            Optional<CodeModel> findBugsXML = getCodeModel(analysisResults, CodeModel.MODEL_TYPES.FINDBUGS_XML);

            result = filteredVulnerabilities(getNodeList(graphXML, "attribute")).map(node ->
                    VulnEntryFactory.getVulnEntry(warnAttributes(node), problemType(node), variableName(findBugsXML, node), nodeName(node))
            ).collect(Collectors.toList());

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

    private NodeList getNodeList(Optional<CodeModel> codeModel, String tagName) throws DataFormatException {
        try {
            Document xml = Utils.getXML(codeModel.get().getModelPath().getAbsolutePath());
            return xml.getElementsByTagName(tagName);
        } catch (ParserConfigurationException | IOException | SAXException e) {
            LOG.error(e);
            throw new DataFormatException("Error occurred while getting nodeList for: " + codeModel + "\ntagName: "+ tagName);
        }
    }

    private Stream<Node> filteredVulnerabilities(NodeList nodeList) {
        return attributes(nodeList).stream().filter(node -> context(node).equals("warning") && problemType(node) != null);
    }

    private String findVariableInFindBugsXML(String vulnType, String lineNum, Optional<CodeModel>  findBugsCM) throws ParserConfigurationException, SAXException, IOException, DataFormatException {
        if (findBugsCM.get().getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return null;

        var bugInstances = attributes(getNodeList(findBugsCM, "BugInstance"));

        for (Node bugInstance : bugInstances) {
            String bugType = getNodeAttribute(bugInstance, "type");
            if (!vulnMap.containsKey(vulnType)) continue; // vuln type is unsupported
            if (!vulnMap.get(vulnType).equals(bugType)) continue;

            String foundLineNum = null;
            Node localVariable = null;

            List<Node> children = nodeListToArrayList(bugInstance.getChildNodes());
            for (Node child : children) {
                // Get line num
                switch (child.getNodeName()) {
                    case "SourceLine":
                        foundLineNum = getNodeAttribute(child, "start");
                        break;

                    case "LocalVariable":
                    case "Field":
                        localVariable = child;
                        break;
                }
            }

            // TODO: clean this up
            if (foundLineNum != null && localVariable == null) {
//                System.out.println("Found " + bugType + " on line " + foundLineNum + " without associated variable!");
                return null;
            }

            if (foundLineNum == null) continue;

            if (foundLineNum.equals(lineNum)) {
                return getNodeAttribute(localVariable, "name");
            }
        }

        return null;
    }


    /** Queries for temp variables */

    private List<Node> attributes(NodeList nodeList) {
        return nodeListToArrayList(nodeList);
    }

    private NodeList warnAttributes(Node node) {
        return node.getChildNodes();
    }

    private String lineNumStr(Node node) {
        return getNodeAttribute(node.getChildNodes().item(3), "value");
    }

    private String problemType(Node node) {
        return supportedProblemTypes.get(nodeName(node));
    }

    private String context(Node node) {
        return getNodeAttribute(node, "context");
    }

    private String nodeName(Node node) {
        return getNodeAttribute(node, "name");
    }

    private String variableName(Optional<CodeModel> findBugsXML, Node node) {
        try {
            return findVariableInFindBugsXML(nodeName(node), lineNumStr(node), findBugsXML);
        } catch (ParserConfigurationException | SAXException | IOException | DataFormatException e) {
            LOG.error(e);
            return "";
        }
    }

}
