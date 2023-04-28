package eu.assuremoss.utils.parsers;

import columbus.ColumbusException;
import columbus.CsiHeader;
import columbus.StrTable;
import columbus.java.asg.Factory;
import columbus.java.asg.Range;
import columbus.java.asg.algorithms.AlgorithmPreorder;
import columbus.java.asg.enums.NodeKind;
import columbus.java.asg.expr.Expression;
import columbus.java.asg.expr.FieldAccess;
import columbus.java.asg.expr.Identifier;
import columbus.java.asg.struc.ClassDeclaration;
import columbus.java.asg.struc.MethodDeclaration;
import columbus.java.asg.struc.Variable;
import columbus.java.asg.struc.VariableDeclaration;
import columbus.java.asg.visitors.VisitorAbstractNodes;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;

public class ASGInfoParser {
    private Factory factory;
    private Map<String, Integer> analyzedClasses;

    public ASGInfoParser(File asgPath) {
        try
        {
            analyzedClasses = collectAnalyzedClasses (asgPath);
        } catch (IOException e) {
            System.err.println("ASG checked is missing");
            e.printStackTrace();
        }
    }
    public static class ClassVisitor extends VisitorAbstractNodes {
        Map<String, Integer> analyzedClasses = new HashMap<String, Integer>();


        @Override
        public void visit(ClassDeclaration node, boolean callVirtualBase) {
            analyzedClasses.put(node.getBinaryName(), node.getId());
        }
    }

    public static class ASGSourceVisitor extends VisitorAbstractNodes {
        VulnerabilityEntry vulnerabilityEntry;
        public ASGSourceVisitor(VulnerabilityEntry vulnerabilityEntry) {
            this.vulnerabilityEntry = vulnerabilityEntry;
        }

        @Override
        public void visit(Variable node, boolean callVirtualBase) {

            switch(vulnerabilityEntry.getType()) {
                case "MS_SHOULD_BE_FINAL":
                case "MS_MUTABLE_ARRAY":
                case "MS_MUTABLE_COLLECTION":
                case "MS_MUTABLE_COLLECTION_PKGPROTECT":
                case "MS_PKGPROTECT":
                    if (node.getName().equals(vulnerabilityEntry.getVariable())) {
                        Range namePosition = node.getNamePosition();
                        vulnerabilityEntry.setStartCol(namePosition.getCol());
                        vulnerabilityEntry.setEndCol(namePosition.getEndCol());
                    }
                    break;
            }
        }

        @Override
        public void visit(FieldAccess node, boolean callVirtualBase) {
            switch(vulnerabilityEntry.getType()) {
                case "NP_NULL_ON_SOME_PATH":
                case "NP_NULL_ON_SOME_PATH_EXCEPTION": {
                    Range range = node.getPosition();
                    if (range.getLine() == vulnerabilityEntry.getStartLine() && range.getEndLine() == vulnerabilityEntry.getEndLine()) {
                        Expression left = node.getLeftOperand();
                        if (left.getNodeKind() == NodeKind.ndkIdentifier) {
                            range = left.getPosition();
                            vulnerabilityEntry.setStartCol(range.getCol());
                            vulnerabilityEntry.setEndCol(range.getEndCol());
                        }
                    }
                    break;
                }
            }
        }

        @Override
        public void visit(Identifier node, boolean callVirtualBase) {
            switch(vulnerabilityEntry.getType()) {
                case "EI_EXPOSE_REP2":
                case "EI_EXPOSE_REP":
                {
                    Range range = node.getPosition();
                    if (node.getName() != null && node.getName().equals(vulnerabilityEntry.getVariable())
                            && range.getLine() == vulnerabilityEntry.getStartLine()
                            && range.getEndLine() == vulnerabilityEntry.getEndLine() )
                    {
                        vulnerabilityEntry.setStartCol(range.getCol());
                        vulnerabilityEntry.setEndCol(range.getEndCol());
                    }
                    break;
                }
            }

        }

        @Override
        public void visit(MethodDeclaration node, boolean callVirtualBase) {
            if ("FI_PUBLIC_SHOULD_BE_PROTECTED".equals(vulnerabilityEntry.getType())) {
                Range range = node.getPosition();

                if (range.getEndLine() >= vulnerabilityEntry.getStartLine() && range.getEndLine()<=vulnerabilityEntry.getEndLine()) {
                    Range namePosition = node.getNamePosition();
                    vulnerabilityEntry.setStartLine(namePosition.getLine());
                    vulnerabilityEntry.setEndLine(namePosition.getEndLine());
                    vulnerabilityEntry.setStartCol(namePosition.getCol());
                    vulnerabilityEntry.setEndCol(namePosition.getEndCol());
                }

            }
        }
    }

    private  Factory loadFactoryToASG(File asgLocation) {
        // creating Factory
        StrTable strTable = new StrTable();
        factory = new Factory(strTable);

        // loading ASG
        CsiHeader csiHeader = new CsiHeader();
        try {
            factory.load(asgLocation.getAbsolutePath(), csiHeader);
        } catch (ColumbusException ex) {
            System.out.println("ERROR: " + ex);
            System.exit(1);
        }

        return factory;
    }

    private Map<String, Integer> collectAnalyzedClasses(File asgFile) throws IOException {
        URI asgURI = null;
        try {
            // load ASG
            MLogger.getActiveLogger().fInfo("Analyzed asg: " + asgFile.getAbsolutePath());
            if (!asgFile.exists() || !asgFile.canRead() || asgFile.isDirectory()) {
                final URL resource = Thread.currentThread().getContextClassLoader().getResource(asgFile.getAbsolutePath());
                if (resource == null) {
                    throw new IOException();
                }
                asgURI = resource.toURI();
            } else {
                asgURI = asgFile.getAbsoluteFile().toURI();
            }

            if (asgFile.getAbsolutePath().endsWith(".ljsi") || asgFile.getAbsolutePath().endsWith(".jsi")) {
                factory = loadFactoryToASG(asgFile);

            }
        } catch (IOException | URISyntaxException e) {
            throw new IOException("IOError: ASG file doesn't exist at the location: " + asgFile.getAbsolutePath(), e);
        }

        ClassVisitor classVisitor = new ClassVisitor();
        AlgorithmPreorder ap = new AlgorithmPreorder();

        if (factory != null)
            ap.run(factory, classVisitor);
        return classVisitor.analyzedClasses;
    }

    public  Map<String, Integer>  getAnalyzedClasses()  {
        return this.analyzedClasses;
    }

    public void vulnarabilityInfoClarification (VulnerabilityEntry vulnEntry) {
        //System.out.println("Entry: " + vulnEntry);
        Integer id = analyzedClasses.get(vulnEntry.getClassName());
        //System.out.println("Clarification: " + vulnEntry.getClassName() + " " + id);
        ASGSourceVisitor visitor = new ASGSourceVisitor(vulnEntry);
        AlgorithmPreorder ap = new AlgorithmPreorder();
        if (factory != null &&id != null)
            ap.run(factory, visitor, id);
    }
}
