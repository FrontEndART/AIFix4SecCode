package eu.assuremoss.utils;

import com.github.javaparser.Range;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import lombok.Builder;

public abstract class ColumnInfoParser {

    public static Pair<Integer, Integer> getColumnInfo(VulnerabilityEntry vulnEntry) {
        String fileContent = Utils.readFileString(vulnEntry.getPath());
        if (fileContent == null) return null;

        CompilationUnit cu = StaticJavaParser.parse(fileContent);
        TraceVisitor traceVisitor = TraceVisitor.builder()
                .lineNum(vulnEntry.getStartLine())
                .variableName(vulnEntry.getVariable())
                .vulnType(vulnEntry.getVulnType())
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
            if (vulnType.equals("FB_MSBF") && range.begin.line == lineNum && name.equals(variableName)) {
                resultRange = range;
            }

            super.visit(node, arg);
        }

        public Pair<Integer, Integer> getResultPair() {
            return new Pair<>(resultRange.begin.column, resultRange.end.column + 1);
        }
    }
}
