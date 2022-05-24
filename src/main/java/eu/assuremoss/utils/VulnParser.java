package eu.assuremoss.utils;

import eu.assuremoss.framework.model.CodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class VulnParser {
    public static final HashMap<String, String> map = new HashMap<String, String>() {{
        put("FB_EiER", "EI_EXPOSE_REP2");
        put("FB_EER", "EI_EXPOSE_REP2");
        put("FB_NNPD", "NP_NULL_PARAM_DEREF");
        put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        put("FB_MSBF", "MS_SHOULD_BE_FINAL");
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

    public static int getColumnInfoFromFindBugsXML(String srcLocation, String vulnType, String lineNum, CodeModel findBugsCM) {
        System.out.println("Trying to find " + vulnType);
        if (findBugsCM.getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return -1;

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
                if (!map.containsKey(vulnType)) continue; // vuln type is unsupported
                if (!map.get(vulnType).equals(bugType)) continue;

                String foundLineNum = null;
                String filePath = null;
                Node localVariable = null;
                NodeList children = node.getChildNodes();
                for (int j = 0; j < children.getLength(); j++) {
                    // Get line num
                    Node child = children.item(j);

                    switch (child.getNodeName()) {
                        case "SourceLine":
                            foundLineNum = child.getAttributes().getNamedItem("start").getNodeValue();
                            filePath = child.getAttributes().getNamedItem("sourcepath").getNodeValue();

                            if (child.getAttributes().getNamedItem("role") == null) break;

                            if (child.getAttributes().getNamedItem("role").getNodeValue().equals("SOURCE_LINE_KNOWN_NULL")) {
                                System.out.println("Known null at line " + foundLineNum + ", returning!");
                                return getColumnInfo(Path.of(srcLocation, filePath), "--null--", Integer.parseInt(foundLineNum));
                            }
                            break;
                        case "LocalVariable":
                        case "Field":
                            localVariable = child;
                            break;
                    }
                }

                if (foundLineNum != null && localVariable == null) {
                    System.out.println("Found " + bugType + " on line " + foundLineNum + " without associated variable!");
                }

                if (foundLineNum == null || localVariable == null) continue;

                if (foundLineNum.equals(lineNum)) {
                    String variableName = localVariable.getAttributes().getNamedItem("name").getNodeValue();
                    System.out.println("FILE: " + Path.of(srcLocation, filePath).toString());
                    System.out.println("Found Vulnerability: " + bugType + " (" + map.get(vulnType) + ")");
                    System.out.println("LineNum: " + lineNum);
                    System.out.println("Variable name: " + variableName);
                    System.out.println("-----------------------------------------");

                    return getColumnInfo(Path.of(srcLocation, filePath), variableName, Integer.parseInt(lineNum));
                }
            }
            // System.out.println(bugInstances);
        } catch (Exception e) {
            e.printStackTrace();
            // System.out.println(e);
        }

        return -1;
    }

    private static int getColumnInfo(Path filePath, String variableName, int lineNum) {
        lineNum -= 1; // line count starts from 1

        ArrayList<String> fileContent = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath.toString()))) {
            while(reader.ready()) {
                fileContent.add(reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }

        String line = null;

        try {
            line = fileContent.get(lineNum);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return -1;
        }

        int index = line.indexOf(variableName);

        while (index != -1 && line.charAt(index-1) == '.') {
            // preceded by a "this." or some other modifier, ignore!
            // TODO: this might mean that the variable is a field; more checks should be set in place

            index = line.indexOf(variableName, index+1);
        }

        if (index == -1) {
            // trim trailing whitespace and set that as column start
            String trimmed = line.trim();

            index = line.indexOf(trimmed);
        }

        return index+1; // +1 because column count starts from 1
    }
}
