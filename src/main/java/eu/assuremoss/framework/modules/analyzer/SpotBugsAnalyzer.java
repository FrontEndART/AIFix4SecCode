package eu.assuremoss.framework.modules.analyzer;

import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import lombok.AllArgsConstructor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
public class SpotBugsAnalyzer implements CodeAnalyzer, VulnerabilityDetector {
    private static final Logger LOG = LogManager.getLogger(SpotBugsAnalyzer.class);

    private String projectName;
    private String spotbugsPath;
    private String resultsPath;


    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation) {
        List<CodeModel> resList = new ArrayList<>();


        String[] command = new String[]{
                "java",
                "-jar",
                new File(spotbugsPath, "spotbugs.jar").getAbsolutePath(),
                "-textui",
                "-xml",
                "-projectName",
                projectName,
                "-output",
                new File(resultsPath, projectName + ".xml").getAbsolutePath(),
                String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "target", "classes"))
        };

        for (String s : command) {
            System.out.print(s + " ");
        }
        System.out.println();

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

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.CFG, new File(resultsPath, projectName + ".xml")));
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
            Document doc = db.parse(new File(resultsPath, projectName + ".xml"));

            NodeList bugInstances = doc.getElementsByTagName("BugInstance");
            for (int i = 0 ; i < bugInstances.getLength(); i++) {
                Node bugInstance = bugInstances.item(i);
                String type = bugInstance.getAttributes().getNamedItem("type").getNodeValue();
                String description = bugInstance.getAttributes().getNamedItem("category").getNodeValue();

                NodeList sourceLines = ((Element) bugInstance).getElementsByTagName("SourceLine");
                Node sourceLine = sourceLines.item(sourceLines.getLength() - 1);
                String file = sourceLine.getAttributes().getNamedItem("sourcepath").getNodeValue();
                String[] splitFile = file.split("/");
                String path = String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "src", "main", "java"));
                for (String s : splitFile) {
                    path = String.valueOf(Paths.get(path, s));
                }
                int startLine = Integer.parseInt(sourceLine.getAttributes().getNamedItem("start").getNodeValue());
                int endLine = Integer.parseInt(sourceLine.getAttributes().getNamedItem("end").getNodeValue());

                VulnerabilityEntry ve = new VulnerabilityEntry();
                ve.setType(type);
                ve.setDescription(description);
                ve.setPath(path);
                ve.setStartLine(startLine);
                ve.setEndLine(endLine);
                ve.setStartCol(0);
                ve.setEndCol(1);

                resList.add(ve);
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        return resList;
    }
}
