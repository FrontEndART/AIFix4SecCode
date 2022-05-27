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
import eu.assuremoss.utils.ColumnInfoParser;
import lombok.AllArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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
    public List<VulnerabilityEntry> getVulnerabilityLocations(File srcLocation, List<CodeModel> analysisResults) {
        for (CodeModel cm : analysisResults) {
            MLOG.info("Location: " + cm.getModelPath());
        }
        List<VulnerabilityEntry> resList = new ArrayList<>();
        Optional<CodeModel> graphXML = analysisResults.stream()
                .filter(cm -> cm.getType() == CodeModel.MODEL_TYPES.OSA_GRAPH_XML).findFirst();
        if (!graphXML.isPresent()) {
            LOG.error("Could not locate GRAPH XML analysis results, no vulnerabilities were retrieved.");
            return resList;
        }

        Optional<CodeModel> findBugsXML = analysisResults.stream()
                .filter(cm -> cm.getType() == CodeModel.MODEL_TYPES.FINDBUGS_XML).findFirst();
        if (!findBugsXML.isPresent()) {
            LOG.error("Could not locate FindBugs XML analysis results, no vulnerabilities were retrieved.");
            return resList;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        ColumnInfoParser columnInfoParser = new ColumnInfoParser();

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(graphXML.get().getModelPath().getAbsolutePath());
            NodeList nodeList = doc.getElementsByTagName("attribute");

            // Convert to a List for better clarity
            List<Node> attributes = nodeListToArrayList(nodeList);

            for (Node node : attributes) {
                String nodeName = getNodeAttribute(node, "name");
                String context = getNodeAttribute(node, "context");
                String problemType = supportedProblemTypes.get(nodeName);

                if (!context.equals("warning")) continue;
                if (problemType == null) continue;

                MLOG.info("Found " + problemType + ", now trying to map column info...");

                // TODO: replace .item(3) with actually finding the LineNum Node
                String filePath = getNodeAttribute(node.getChildNodes().item(1), "value");
                String lineNumStr = getNodeAttribute(node.getChildNodes().item(3), "value");

                Pair<Integer, Integer> columnInfo = columnInfoParser
                        .getColumnInfoFromFindBugsXML(filePath, nodeName, lineNumStr, findBugsXML.get());
                MLOG.info("Line: " + lineNumStr + ", Column: [" + columnInfo.getA() + ", " + columnInfo.getB() + "]");

                NodeList warnAttributes = node.getChildNodes();
                resList.add(createVulnerabilityEntry(warnAttributes, problemType, columnInfo));
            }

            /*for (int i = 0; i < attributes.getLength(); i++) {
                String nodeName = attributes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                String nodeContext = attributes.item(i).getAttributes().getNamedItem("context").getNodeValue();
                if ("warning".equals(nodeContext) && supportedProblemTypes.containsKey(nodeName)) {
                    NodeList warnAttributes = attributes.item(i).getChildNodes();
                    String problemType = supportedProblemTypes.get(nodeName);
                    resList.add(createVulnerabilityEntry(warnAttributes, problemType));
                    MLOG.info("Found " + problemType + ", now trying to map column info...");

                    // TODO: replace .item(3) with actually finding the LineNum Node
                    String lineNumStr = attributes.item(i).getChildNodes().item(3).getAttributes().getNamedItem("value").getNodeValue();

                    int columnInfo = columnInfoParser.getColumnInfoFromFindBugsXML(nodeName, lineNumStr, findBugsXML.get());
                    MLOG.info("Line: " + lineNumStr + ", Column: " + columnInfo);
                }
            }*/
        } catch (FileNotFoundException e) {
            LOG.error(e);
        } catch (IOException e) {
            LOG.error(e);
        } catch (ParserConfigurationException e) {
            LOG.error(e);
        } catch (SAXException e) {
            LOG.error(e);
        }

        return resList;
    }

    private VulnerabilityEntry createVulnerabilityEntry(NodeList warnAttributes, String problemType, Pair<Integer, Integer> columnInfo) {
        VulnerabilityEntry ve = new VulnerabilityEntry();

        ve.setType(problemType);

        for (int j = 0; j < warnAttributes.getLength(); j++) {
            if (warnAttributes.item(j).getAttributes() != null) {
                String attrType = warnAttributes.item(j).getAttributes().getNamedItem("name").getNodeValue();
                if ("ExtraInfo".equals(attrType)) {
                    continue;
                }
                String attrVal = warnAttributes.item(j).getAttributes().getNamedItem("value").getNodeValue();
                switch (attrType) {
                    case "Path":
                        ve.setPath(attrVal);
                        break;
                    case "Line":
                        ve.setStartLine(Integer.parseInt(attrVal));
                        break;
                    case "Column":
                        //ve.setStartCol(24);
                        break;
                    case "EndLine":
                        ve.setEndLine(Integer.parseInt(attrVal));
                        break;
                    case "EndColumn":
                        //ve.setEndCol(61);
                        break;
                    case "WarningText":
                        ve.setDescription(attrVal);
                        break;
                }
            }
        }
        // Workaround while VSCode visualization is not fixed
        ve.setStartCol(columnInfo.getA());
        ve.setEndCol(columnInfo.getB());
        //alignLineAndColNumbers(ve);
        return ve;
    }

    private void alignLineAndColNumbers(VulnerabilityEntry ve) {
        switch (ve.getStartLine()) {
            case 3:
                ve.setStartCol(26);
                ve.setEndCol(37);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 7:
                ve.setStartCol(20);
                ve.setEndCol(23);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 12:
                ve.setStartCol(16);
                ve.setEndCol(20);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 16:
                ve.setStartCol(21);
                ve.setEndCol(25);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 24:
                ve.setStartCol(34);
                ve.setEndCol(51);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 29:
                ve.setStartCol(36);
                ve.setEndCol(55);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 34:
                ve.setStartCol(39);
                ve.setEndCol(61);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
            case 40:
                ve.setStartCol(24);
                ve.setEndCol(31);
                MLOG.info("Line " + ve.getStartLine() + ": " + ve.getType());
                MLOG.info("Column start:" + ve.getStartCol() + ", end: " + ve.getEndCol());
                break;
        }
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation,
                analyzeSourceCode(srcLocation, true));
        return !vulnerabilities.contains(ve);
    }
}
