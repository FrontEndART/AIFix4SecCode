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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.*;

public class ColumnInfoParser {
    public static final HashMap<String, String> map = new HashMap<String, String>() {{
        put("FB_EiER", "EI_EXPOSE_REP2");
        put("FB_EER", "EI_EXPOSE_REP2");
        put("FB_NNPD", "NP_NULL_PARAM_DEREF");
        put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        put("FB_MSBF", "MS_SHOULD_BE_FINAL");
    }};

    private ASTParser parser;

    public ColumnInfoParser() {
        setupASTParser();
    }

    private void setupASTParser() {
        // Set up ASTParser
        parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setKind(ASTParser.K_STATEMENTS);
        parser.setBindingsRecovery(true);
        Map options = JavaCore.getOptions();
        parser.setCompilerOptions(options);
        parser.setUnitName("test");
    }

    public Pair<Integer, Integer> getColumnInfoFromFindBugsXML(String filePath, String vulnType, String lineNum, CodeModel findBugsCM) {
        System.out.println("-----------------------------------------");
        System.out.println("FILE: " + filePath);
        String variableName = findVariableInFindBugsXML(vulnType, lineNum, findBugsCM);
        return getColumnInfo(filePath, variableName, lineNum);
    }

    private boolean hasNodeAttribute(Node node, String key) {
        return node.getAttributes().getNamedItem(key) != null;
    }

    private String getNodeAttribute(Node node, String key) {
        return node.getAttributes().getNamedItem(key).getNodeValue();
    }

    private List<Node> nodeListToArrayList(NodeList nodeList) {
        return IntStream.range(0, nodeList.getLength())
                .mapToObj(nodeList::item)
                .collect(Collectors.toList());
    }

    public String findVariableInFindBugsXML(String vulnType, String lineNum, CodeModel findBugsCM) {
        System.out.println("Trying to find " + vulnType);
        if (findBugsCM.getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return "--null--";

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(findBugsCM.getModelPath().getAbsolutePath());

            List<Node> bugInstances = nodeListToArrayList(doc.getElementsByTagName("BugInstance"));
            for (Node bugInstance : bugInstances) {
                String bugType = getNodeAttribute(bugInstance, "type");
                if (!map.containsKey(vulnType)) continue; // vuln type is unsupported
                if (!map.get(vulnType).equals(bugType)) continue;

                String foundLineNum = null;
                Node localVariable = null;

                List<Node> children = nodeListToArrayList(bugInstance.getChildNodes());
                for (Node child : children) {
                    // Get line num
                    switch (child.getNodeName()) {
                        case "SourceLine":
                            foundLineNum = getNodeAttribute(child, "start");

                            if (!hasNodeAttribute(child, "role")) break;
                            if (!getNodeAttribute(child, "role").equals("SOURCE_LINE_KNOWN_NULL")) break;

                            // Known Null errors have no associated variable, return
                            System.out.println("Known null at line " + foundLineNum + ", returning!");
                            return "--null--";

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
                    String variableName = getNodeAttribute(localVariable, "name");
                    System.out.println("Found Vulnerability: " + bugType + " (" + map.get(vulnType) + ")");
                    System.out.println("LineNum: " + lineNum);
                    System.out.println("Variable name: " + variableName);

                    return variableName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "--null--";
    }

    private Pair<Integer, Integer> getColumnInfo(String filePath, String variableName, String lineNumStr) {
        int lineNum = Integer.parseInt(lineNumStr);
        lineNum -= 1; // line count starts from 1, start it from 0

        ArrayList<String> fileContent = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while(reader.ready()) {
                fileContent.add(reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String line;

        try {
            line = fileContent.get(lineNum);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
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

        return new Pair<>(index+1, 0); // +1 because column count starts from 1

    }
}
