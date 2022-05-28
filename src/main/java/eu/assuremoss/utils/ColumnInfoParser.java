package eu.assuremoss.utils;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.utils.CodeGenerationUtils;
import com.github.javaparser.utils.SourceRoot;
import lombok.Builder;

public abstract class ColumnInfoParser {

    public static Pair<Integer, Integer> getColumnInfoFromFindBugsXML(String filePath, String vulnType, String lineNum, String variableName) {
        Pair<Integer, Integer> columnInfo = getColumnInfo(vulnType, filePath, variableName, Integer.parseInt(lineNum));
        System.out.println("=> column: [" + columnInfo.getA() + ", " + columnInfo.getB() + "]");
        return columnInfo;
    }

    private static Pair<Integer, Integer> getColumnInfo(String vulnType, String filePath, String variableName, int lineNum) {
        String fileContent = Utils.readFileString(filePath);
        if (fileContent == null) return null;

        CompilationUnit cu = StaticJavaParser.parse(fileContent);
        TraceVisitor traceVisitor = TraceVisitor.builder()
                .lineNum(lineNum)
                .variableName(variableName)
                .vulnType(vulnType)
                .build();

        cu.accept(traceVisitor, null);

        return traceVisitor.getResultPair();
    }

    @Builder
    private static class TraceVisitor extends VoidVisitorAdapter<Void> {
        private int lineNum;
        private String variableName;
        private String vulnType;
        private Range resultRange;

        @Override
        public void visit(NameExpr node, Void arg) {
            String name = node.getNameAsString();
            Range range = node.getRange().get();

            // TODO: clean this up
            if ((vulnType.equals("FB_EiER") || vulnType.equals("FB_EER") || vulnType.equals("FB_NNOSP")) && range.begin.line == lineNum && (name.equals(variableName) || variableName == null)) {
//                System.out.println("==============");
//                System.out.println("NameExpr: " + node);
//                System.out.println("Name: " + node.getName());
//                System.out.println("Column range: " + range);
//                System.out.println("==============");
                resultRange = range;
            }

            super.visit(node, arg);
        }

        @Override
        public void visit(VariableDeclarator node, Void arg) {
            SimpleName nameNode = node.getName();
            String name = nameNode.getIdentifier();
            Range range = nameNode.getRange().get();

            // TODO: clean this up
            if (vulnType.equals("FB_MSBF") && range.begin.line == lineNum && name.equals(variableName)) {
//                System.out.println("==============");
//                System.out.println("VariableDeclarator: " + node);
//                System.out.println("Name: " + node.getNameAsString());
//                System.out.println("Column range: " + range);
//                System.out.println("==============");
                resultRange = range;
            }

            super.visit(node, arg);
        }

        public Pair<Integer, Integer> getResultPair() {
            return new Pair<>(resultRange.begin.column, resultRange.end.column+1);
        }
    }
}
