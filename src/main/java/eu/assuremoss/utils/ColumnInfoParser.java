package eu.assuremoss.utils;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import eu.assuremoss.framework.model.CodeModel;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.*;

import static eu.assuremoss.utils.Utils.getNodeAttribute;
import static eu.assuremoss.utils.Utils.nodeListToArrayList;

public abstract class ColumnInfoParser {
    public static final HashMap<String, String> vulnMap = new HashMap<>() {{
        put("FB_EiER", "EI_EXPOSE_REP2");
        put("FB_EER", "EI_EXPOSE_REP2");
        put("FB_NNPD", "NP_NULL_PARAM_DEREF");
        put("FB_NNOSP", "NP_NULL_ON_SOME_PATH");
        put("FB_MSBF", "MS_SHOULD_BE_FINAL");
    }};

    public static Pair<Integer, Integer> getColumnInfoFromFindBugsXML(String filePath, String vulnType, String lineNum, CodeModel findBugsCM) {
        System.out.println("-----------------------------------------");
        System.out.println("FILE: " + filePath);
        String variableName = findVariableInFindBugsXML(vulnType, lineNum, findBugsCM);
        return getColumnInfo(vulnType, filePath, variableName, lineNum);
    }

    public static String findVariableInFindBugsXML(String vulnType, String lineNum, CodeModel findBugsCM) {
        if (findBugsCM.getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return null;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db;
        Document doc;

        try {
            // process XML securely, avoid attacks like XML External Entities (XXE)
            dbf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            db = dbf.newDocumentBuilder();
            doc = db.parse(findBugsCM.getModelPath().getAbsolutePath());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

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
                        break;

                    case "LocalVariable":
                    case "Field":
                        localVariable = child;
                        break;
                }
            }

            if (foundLineNum != null && localVariable == null) {
                System.out.println("Found " + bugType + " on line " + foundLineNum + " without associated variable!");
                return null;
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

        return null;
    }

    private static class TraceVisitor extends VoidVisitorAdapter<Void> {
        private int lineNum;
        private String variableName;
        private String lineNumStr;
        private String vulnType;
        private Range resultRange;
        public TraceVisitor setLineNum(int lineNum) {
            this.lineNum = lineNum;
            this.lineNumStr = String.valueOf(lineNum);
            return this;
        }
        public TraceVisitor setVariableName(String variableName) {
            this.variableName = variableName;
            return this;
        }
        public TraceVisitor setVulnType(String vulnType) {
            this.vulnType = vulnType;
            return this;
        }
        public Pair getResultPair() {
            return new Pair<>(resultRange.begin.column, resultRange.end.column+1);
        }
        @Override
        public void visit(NameExpr node, Void arg) {
            String name = node.getNameAsString();
            Range range = node.getRange().get();

            if ((vulnType.equals("FB_EiER") || vulnType.equals("FB_EER") || vulnType.equals("FB_NNOSP")) && range.begin.line == lineNum && (name.equals(variableName) || variableName == null)) {
                System.out.println("==============");
                System.out.println("NameExpr: " + node);
                System.out.println("Name: " + node.getName());
                System.out.println("Column range: " + range);
                System.out.println("==============");
                resultRange = range;
            }

            super.visit(node, arg);
        }

        @Override
        public void visit(VariableDeclarator node, Void arg) {
            SimpleName nameNode = node.getName();
            String name = nameNode.getIdentifier();
            Range range = nameNode.getRange().get();

            if (vulnType.equals("FB_MSBF") && range.begin.line == lineNum && name.equals(variableName)) {
                System.out.println("==============");
                System.out.println("VariableDeclarator: " + node);
                System.out.println("Name: " + node.getNameAsString());
                System.out.println("Column range: " + range);
                System.out.println("==============");
                resultRange = range;
            }

            super.visit(node, arg);
        }
    }

    private static Pair<Integer, Integer> getColumnInfo(String vulnType, String filePath, String variableName, String lineNumStr) {
        int lineNum = Integer.parseInt(lineNumStr);

        String fileContent = Utils.readFileString(filePath);
        if (fileContent == null) return null;

        // Parse the code using JavaParser, as usual
        CompilationUnit cu = StaticJavaParser.parse(fileContent);
        TraceVisitor traceVisitor = new TraceVisitor()
                .setLineNum(lineNum)
                .setVariableName(variableName)
                .setVulnType(vulnType);

        cu.accept(traceVisitor, null);

        return traceVisitor.getResultPair();
    }
}
