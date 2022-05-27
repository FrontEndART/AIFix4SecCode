package eu.assuremoss.utils;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.AssignExpr;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.printer.lexicalpreservation.LexicalPreservingPrinter;
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
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class ColumnInfoParser {
    public static final HashMap<String, String> vulnMap = new HashMap<>() {{
        put("FB_EiER", "EI_EXPOSE_REP2");
        put("FB_EER", "EI_EXPOSE_REP2");
        put("FB_NNPD", "NP_NULL_PARAM_DEREF");
        put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        put("FB_MSBF", "MS_SHOULD_BE_FINAL");
    }};

    /*private static final HashMap<Class, String> acceptedASTNodes = new HashMap<>() {{
        put(MethodInvocation.class, "MethodInvocation");
        put(Assignment.class, "Assignment");
        put(VariableDeclarationExpression.class, "VariableDeclarationExpression");
        put(ReturnStatement.class, "ReturnStatement");
        put(VariableDeclarationFragment.class, "VariableDeclarationFragment");
    }};*/

    public ColumnInfoParser() {
    }

    public Pair<Integer, Integer> getColumnInfoFromFindBugsXML(String filePath, String vulnType, String lineNum, CodeModel findBugsCM) {
        System.out.println("-----------------------------------------");
        System.out.println("FILE: " + filePath);
        String variableName = findVariableInFindBugsXML(vulnType, lineNum, findBugsCM);
        return getColumnInfo(vulnType, filePath, variableName, lineNum);
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

                            if (!hasNodeAttribute(child, "role")) break;
                            if (!getNodeAttribute(child, "role").equals("SOURCE_LINE_KNOWN_NULL")) break;

                            // Known Null errors have no associated variable, return
                            System.out.println("Known null at line " + foundLineNum + ", returning!");
                            return null;

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
                    System.out.println("Found Vulnerability: " + bugType + " (" + vulnMap.get(vulnType) + ")");
                    System.out.println("LineNum: " + lineNum);
                    System.out.println("Variable name: " + variableName);

                    return variableName;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    private Pair<Integer, Integer> getColumnInfo(String vulnType, String filePath, String variableName, String lineNumStr) {
        int lineNum = Integer.parseInt(lineNumStr);

        ArrayList<String> fileContent = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            while(reader.ready()) {
                fileContent.add(reader.readLine());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        String lineSrc;
        String contentStr = "";

        for (String line : fileContent) {
            contentStr += line + "\n";
        }

        try {
            lineSrc = fileContent.get(lineNum);
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return null;
        }

        // you have to parse the code using JavaParser, as usual
        CompilationUnit cu = StaticJavaParser.parse(contentStr);
        // now you attach a LexicalPreservingPrinter tso the AST

        String sourceCode = LexicalPreservingPrinter.print(cu);

        //System.out.println(sourceCode);

        processNodeTree(cu, lineNum, vulnType, variableName);

        return new Pair<>(0,0);
    }

    private Pair<Integer, Integer> processNodeTree(com.github.javaparser.ast.Node node, int lineNum, String vulnType, String variableName) {
        Range range = node.getRange().get();

        if (node instanceof NameExpr) {
            NameExpr expression = (NameExpr) node;

            String name = expression.getNameAsString();
            //System.out.println(vulnType + " == " + "FB_EiER");
            //System.out.println(range.begin.line + " == " + lineNum);
            //System.out.println(name + " == " + variableName);
            if ((vulnType.equals("FB_EiER") || vulnType.equals("FB_EER") || vulnType.equals("FB_NNOSP")) && range.begin.line == lineNum && (name.equals(variableName) || variableName == null)) {
                System.out.println("==============");
                System.out.println("NameExpr: " + expression);
                System.out.println("Name: " + expression.getName());
                System.out.println("Column range: " + range);
                System.out.println("==============");
                return new Pair<>(range.begin.column, range.end.column+1);
            }
        }

        if (node instanceof VariableDeclarator) {
            VariableDeclarator declarator = (VariableDeclarator) node;
            SimpleName nameNode = declarator.getName();
            String name = nameNode.getIdentifier();
            range = nameNode.getRange().get();

            //System.out.println(vulnType + " == " + "FB_MSBF");
            //System.out.println(range.begin.line + " == " + lineNum);
            //System.out.println(name + " == " + variableName);
            if (vulnType.equals("FB_MSBF") && range.begin.line == lineNum && name.equals(variableName)) {
                System.out.println("==============");
                System.out.println("VariableDeclarator: " + declarator);
                System.out.println("Name: " + declarator.getNameAsString());
                System.out.println("Column range: " + range);
                System.out.println("==============");
                return new Pair<>(range.begin.column, range.end.column+1);
            }
        }

        if (node.getChildNodes() != null) {
            for (com.github.javaparser.ast.Node child:node.getChildNodes()) {
                Pair<Integer, Integer> childReturnValue = processNodeTree(child, lineNum, vulnType, variableName);
                if (childReturnValue != null) {
                    // We caught a correct output that isnt null, return it all the way to the caller
                    return childReturnValue;
                }
            }
        }

        return null;
    }
}
