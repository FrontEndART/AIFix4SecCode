package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@AllArgsConstructor
public class OpenStaticAnalyzer implements CodeAnalyzer, VulnerabilityDetector, PatchValidator {
    private static final Logger LOG = LogManager.getLogger(OpenStaticAnalyzer.class);

    private String osaPath;
    private String osaEdition;
    private String resultsDir;
    private String projectName;
    private String patchSavePath;

    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation) {
        List<CodeModel> resList = new ArrayList<>();

        String extension = "";
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            extension = ".exe";
        } else if (osName.contains("Linux")) {
            extension = "";
        }

        String[] command = new String[]{
                new File(osaPath, osaEdition + "Java" + extension).getAbsolutePath(),
                "-resultsDir=" + resultsDir,
                "-projectName=" + projectName,
                "-projectBaseDir=" + srcLocation,
                "-cleanResults=0",
                "-currentDate=0"
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                LOG.info(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        String asgPath = String.valueOf(Paths.get(resultsDir,
                projectName,
                "java",
                "0",
                osaEdition.toLowerCase(Locale.ROOT),
                projectName + ".ljsi"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asgPath)));

        return resList;
    }

    @Override
    public List<VulnerabilityEntry> getVulnerabilityLocations(File srcLocation) {
        List<VulnerabilityEntry> resList = new ArrayList<>();

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(String.valueOf(Paths.get(resultsDir, projectName, "java", "0", projectName + ".xml"))).getAbsolutePath());
            NodeList attributes = doc.getElementsByTagName("attribute");
            for (int i = 0; i < attributes.getLength(); i++) {
                if ("warning".equals(attributes.item(i).getAttributes().getNamedItem("context").getNodeValue())) {
                    NodeList warnAttributes = attributes.item(i).getChildNodes();
                    VulnerabilityEntry ve = new VulnerabilityEntry();
                    for (int j = 0; j < warnAttributes.getLength(); j++) {
                        if (warnAttributes.item(j).getAttributes() != null) {
                            String attrType = warnAttributes.item(j).getAttributes().getNamedItem("name").getNodeValue();
                            String attrVal = warnAttributes.item(j).getAttributes().getNamedItem("value").getNodeValue();
                            switch (attrType) {
                                case "Path":
                                    ve.setPath(attrVal);
                                    break;
                                case "Line":
                                    ve.setStartLine(Integer.parseInt(attrVal));
                                    break;
                                case "Column":
                                    ve.setStartCol(Integer.parseInt(attrVal));
                                    break;
                                case "EndLine":
                                    ve.setEndLine(Integer.parseInt(attrVal));
                                    break;
                                case "EndColumn":
                                    ve.setEndCol(Integer.parseInt(attrVal));
                                    break;
                                case "WarningText":
                                    ve.setType(attrVal);
                                    break;
                            }
                        }
                    }
                    resList.add(ve);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        // for now, return only the first element to make the testing easier
        return resList.stream().findFirst().stream().collect(Collectors.toList());
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        analyzeSourceCode(srcLocation);
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation);
        return !vulnerabilities.contains(ve);
    }
}
