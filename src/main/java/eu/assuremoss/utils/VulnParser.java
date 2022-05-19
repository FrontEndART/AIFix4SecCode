package eu.assuremoss.utils;

import eu.assuremoss.framework.model.CodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

public class VulnParser {
    public static final HashMap<String, String> map = new HashMap<String, String>() {{
        put("EI_EXPOSE_REP2", "FB_EiER");
        put("NP_NULL_PARAM_DEREF", "FB_NNPD");
        put("NP_NULL_ON_SOME_PATH", "FB_NNOSP");
    }};

    public static boolean compare(String from, String to) {
        if (map.containsKey(from)) {
            return map.get(from).equals(to);
        }
        if (map.containsKey(to)) {
            return map.get(to).equals(from);
        }

        return false;
    }

    public static String findVulnVariableInFindBugsXML(String vulnType, String lineNum, CodeModel findBugsCM) {
        if (findBugsCM.getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(findBugsCM.getModelPath().getAbsolutePath());
            NodeList bugInstances = doc.getElementsByTagName("BugInstance");
            for (int i = 0; i < bugInstances.getLength(); i++) {
                Node node = bugInstances.item(i);
                String bugType = node.getAttributes().getNamedItem("type").getNodeValue();
                if (!map.containsKey(bugType)) continue; // bug type is unsupported
                if (!map.get(bugType).equals(vulnType)) continue;

                String foundLineNum = null;
                Node localVariable = null;
                NodeList children = node.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    // Get line num
                    Node child = children.item(j);

                    switch (child.getNodeName()) {
                        case "SourceLine":
                            foundLineNum = child.getAttributes().getNamedItem("start").getNodeValue();
                            break;
                        case "LocalVariable":
                            localVariable = child;
                            break;
                    }
                }

                if (foundLineNum == null || localVariable == null) continue;

                if (foundLineNum.equals(lineNum)) {
                    System.out.println("Found Vulnerability: " + bugType + " (" + map.get(bugType) + ")");
                    System.out.println("LineNum: " + lineNum);
                    System.out.println("Variable name: " + localVariable.getAttributes().getNamedItem("name").getNodeValue());
                    System.out.println("-----------------------------------------");
                }
            }
            // System.out.println(bugInstances);
        } catch (Exception e) {
            System.out.println(e);
        }

        return null;
    }
}
