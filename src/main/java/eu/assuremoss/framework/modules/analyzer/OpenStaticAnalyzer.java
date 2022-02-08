package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.utils.Extension;
import eu.assuremoss.utils.Pair;
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

@AllArgsConstructor
public class OpenStaticAnalyzer implements CodeAnalyzer, VulnerabilityDetector, PatchValidator {
    private static final Logger LOG = LogManager.getLogger(OpenStaticAnalyzer.class);

    private static final Map<String, String> SUPPORTED_PROBLEM_TYPES = new HashMap<>();
    static {
        SUPPORTED_PROBLEM_TYPES.put("FB_EiER", "");
        SUPPORTED_PROBLEM_TYPES.put("FB_MSBF", "MS_SHOULD_BE_FINAL");
        SUPPORTED_PROBLEM_TYPES.put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        SUPPORTED_PROBLEM_TYPES.put("FB_NNOSPE", "NP_NULL_ON_SOME_PATH_EXCEPTION");
    }

    private String osaPath;
    private String osaEdition;
    private String j2cpPath;
    private String j2cpEdition;
    private String resultsPath;
    private String projectName;
    private String patchSavePath;

    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation) {
        MavenPatchCompiler mpc = new MavenPatchCompiler();
        mpc.compile(srcLocation, true, true);

        String fbFileListPath = String.valueOf(Paths.get(resultsPath, "fb_file_list.txt"));
        try (FileWriter fw = new FileWriter(fbFileListPath);) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "target", "classes")));
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<CodeModel> resList = new ArrayList<>();

        String[] command = new String[]{
                new File(osaPath, osaEdition + "Java" + Extension.getExtension()).getAbsolutePath(),
                "-resultsDir=" + resultsPath,
                "-projectName=" + projectName,
                "-projectBaseDir=" + srcLocation,
                "-cleanResults=0",
                "-currentDate=0",
                "-FBFileList=" + fbFileListPath,
                "-runFB=true",
                "-FBOptions=-auxclasspath " + String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "target", "dependency"))
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

        String asgPath = String.valueOf(Paths.get(resultsPath,
                projectName,
                "java",
                "0",
                osaEdition.toLowerCase(Locale.ROOT),
                "asg",
                projectName + ".ljsi"));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asgPath)));

        command = new String[] {
                new File(j2cpPath, j2cpEdition + Extension.getExtension()).getAbsolutePath(),
                asgPath,
                "-from:" + Paths.get(srcLocation.getAbsolutePath(), "src", "main", "java") + File.separator,
                "-to:"
        };
        processBuilder = new ProcessBuilder(command);
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
            Document doc = db.parse(new File(String.valueOf(Paths.get(resultsPath, projectName, "java", "0", projectName + ".xml"))).getAbsolutePath());
            NodeList attributes = doc.getElementsByTagName("attribute");
            for (int i = 0; i < attributes.getLength(); i++) {
                String nodeName = attributes.item(i).getAttributes().getNamedItem("name").getNodeValue();
                String nodeContext = attributes.item(i).getAttributes().getNamedItem("context").getNodeValue();
                if ("warning".equals(nodeContext) && SUPPORTED_PROBLEM_TYPES.containsKey(nodeName)) {
                    NodeList warnAttributes = attributes.item(i).getChildNodes();
                    String problemType = SUPPORTED_PROBLEM_TYPES.get(nodeName);
                    if ("".equals(problemType)) {
                        resList.add(createVulnerabilityEntry(warnAttributes, "EI_EXPOSE_REP2"));
                        resList.add(createVulnerabilityEntry(warnAttributes, "EI_EXPOSE_REP2_ARRAY"));
                        resList.add(createVulnerabilityEntry(warnAttributes, "EI_EXPOSE_REP2_DATEOBJECT"));
                    } else {
                        resList.add(createVulnerabilityEntry(warnAttributes, problemType));
                    }
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
                        ve.setStartCol(Integer.parseInt(attrVal));
                        break;
                    case "EndLine":
                        ve.setEndLine(Integer.parseInt(attrVal));
                        break;
                    case "EndColumn":
                        ve.setEndCol(Integer.parseInt(attrVal));
                        break;
                    case "WarningText":
                        ve.setDescription(attrVal);
                        break;
                }
            }
        }
        return ve;
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        analyzeSourceCode(srcLocation);
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation);
        return !vulnerabilities.contains(ve);
    }
}
