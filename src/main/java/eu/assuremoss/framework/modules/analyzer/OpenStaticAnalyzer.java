package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
import lombok.AllArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

import static eu.assuremoss.VulnRepairDriver.MLOG;

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
        MavenPatchCompiler mpc = new MavenPatchCompiler();
        mpc.compile(srcLocation, true, true);

        String workingDir = isValidation ? validation_results_path : resultsPath;

        String fbFileListPath = String.valueOf(Paths.get(workingDir, "fb_file_list.txt"));
        try (FileWriter fw = new FileWriter(fbFileListPath)) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "target", "classes")));
        } catch (IOException e) {
            LOG.error(e);
        }

        List<CodeModel> resList = new ArrayList<>();

        String[] command = new String[]{
                new File(osaPath, osaEdition + "Java" + Utils.getExtension()).getAbsolutePath(),
                "-resultsDir=" + workingDir,
                "-projectName=" + projectName,
                "-projectBaseDir=" + srcLocation,
                "-cleanResults=0",
                "-currentDate=0",
                "-FBFileList=" + fbFileListPath,
                "-runFB=true",
                "-FBOptions=-auxclasspath " + Paths.get(srcLocation.getAbsolutePath(), "target", "dependency")
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        runProcess(processBuilder);

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

        command = new String[]{
                new File(j2cpPath, j2cpEdition + Utils.getExtension()).getAbsolutePath(),
                asgPath,
                "-from:" + Paths.get(srcLocation.getAbsolutePath(), "src", "main", "java") + File.separator,
                "-to:"
        };
        processBuilder = new ProcessBuilder(command);
        runProcess(processBuilder);

        return resList;
    }

    private void runProcess(ProcessBuilder processBuilder) {
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                MLOG.fInfo(line);
            }
        } catch (IOException e) {
            LOG.error(e);
        }
    }

    @Override
    public List<VulnerabilityEntry> getVulnerabilityLocations(File srcLocation, List<CodeModel> analysisResults) {
        List<VulnerabilityEntry> resList = new ArrayList<>();
        Optional<CodeModel> graphXML = analysisResults.stream().filter(cm -> cm.getType() == CodeModel.MODEL_TYPES.OSA_GRAPH_XML).findFirst();
        if (!graphXML.isPresent()) {
            LOG.error("Could not locate GRAPH XML analysis results, no vulnerabilities were retrieved.");
            return resList;
        }

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(graphXML.get().getModelPath().getAbsolutePath());
            NodeList attributes = doc.getElementsByTagName("attribute");
            for (int i = 0; i < attributes.getLength(); i++) {
                String nodeName = attributes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                String nodeContext = attributes.item(i).getAttributes().getNamedItem("context").getNodeValue();
                if ("warning".equals(nodeContext) && supportedProblemTypes.containsKey(nodeName)) {
                    NodeList warnAttributes = attributes.item(i).getChildNodes();
                    String problemType = supportedProblemTypes.get(nodeName);
                    resList.add(createVulnerabilityEntry(warnAttributes, problemType));
                }
            }
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

    private VulnerabilityEntry createVulnerabilityEntry(NodeList warnAttributes, String problemType) {
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
                        ve.setStartCol(24);
                        break;
                    case "EndLine":
                        ve.setEndLine(Integer.parseInt(attrVal));
                        break;
                    case "EndColumn":
                        ve.setEndCol(61);
                        break;
                    case "WarningText":
                        ve.setDescription(attrVal);
                        break;
                }
            }
        }
        // Workaround while VSCode visualization is not fixed
        alignLineAndColNumbers(ve);
        return ve;
    }

    private void alignLineAndColNumbers(VulnerabilityEntry ve) {
        switch (ve.getStartLine()) {
            case 3:
                ve.setStartCol(26);
                ve.setEndCol(37);
                break;
            case 7:
                ve.setStartCol(20);
                ve.setEndCol(23);
                break;
            case 12:
                ve.setStartCol(16);
                ve.setEndCol(20);
                break;
            case 16:
                ve.setStartCol(21);
                ve.setEndCol(25);
                break;
            case 24:
                ve.setStartCol(34);
                ve.setEndCol(51);
                break;
            case 29:
                ve.setStartCol(36);
                ve.setEndCol(55);
                break;
            case 34:
                ve.setStartCol(39);
                ve.setEndCol(61);
                break;
            case 40:
                ve.setStartCol(24);
                ve.setEndCol(31);
                break;
        }
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation, analyzeSourceCode(srcLocation, true));
        return !vulnerabilities.contains(ve);
    }
}
