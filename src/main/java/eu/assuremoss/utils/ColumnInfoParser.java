package eu.assuremoss.utils;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import lombok.Builder;

public abstract class ColumnInfoParser {

   /* public static Pair<Integer, Integer> getColumnInfo(VulnerabilityEntry vulnEntry) {
        String fileContent = Utils.readFileString(vulnEntry.getPath());
        if (fileContent == null) return null;

        CompilationUnit cu = StaticJavaParser.parse(fileContent);
        TraceVisitor traceVisitor = TraceVisitor.builder()
                .lineNum(vulnEntry.getStartLine())
                .variableName(vulnEntry.getVariable())
                .vulnType(vulnEntry.getVulnType())
                .vulnEntry(vulnEntry)
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
        private VulnerabilityEntry vulnEntry;

        @Override
        public void visit(NameExpr node, Void arg) {
            String name = node.getNameAsString();
            Range range = node.getRange().get();

            // TODO: clean this up
            if ((vulnType.equals("FB_EiER") || vulnType.equals("FB_EER") || vulnType.equals("FB_NNOSP"))
                    && range.begin.line == lineNum && (name.equals(variableName) || variableName == null)) {
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
            if (variableName !=  null && (vulnType.equals("FB_MSBF")  || vulnType.equals("FB_MMC") || vulnType.equals("FB_MP") )&& range.begin.line == lineNum && (name.equals(variableName) || variableName.endsWith("."+name))) {
                resultRange = range;
            }

           super.visit(node, arg);
        }

        @Override
        public void visit(MethodDeclaration node, Void arg) {
            if (!vulnType.equals("FB_FSBP"))
                super.visit(node, arg);
            SimpleName nameNode = node.getName();
            String name = nameNode.getIdentifier();
            Range range = nameNode.getRange().get();

            if (vulnType.equals("FB_FSBP")) {
                variableName = "finalize";
                vulnEntry.setStartLine(range.begin.line);
            }
            if (variableName !=  null && vulnType.equals("FB_FSBP") && range.begin.line >= lineNum-2 && (name.equals(variableName) || variableName.endsWith("."+name))) {
                resultRange = node.getRange().get();
            }
            super.visit(node, arg);
        }
        public Pair<Integer, Integer> getResultPair() {
            Pair<Integer, Integer> result;

            try {
                result = new Pair<>(resultRange.begin.column, resultRange.end.column + 1);
            } catch (NullPointerException npe) {
                result = new Pair<>(-1, -1);
            }

            return result;
        }
    }*/
}
