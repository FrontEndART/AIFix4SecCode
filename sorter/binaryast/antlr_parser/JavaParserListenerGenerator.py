from JavaParser import JavaParser


class JavaParserListenerGenerator:
    permittedRuleNames = [
        "compilationUnit",
        "packageDeclaration",
        "importDeclaration",
        "classDeclaration",
        "typeParameter",
        "typeBound",
        "enumDeclaration",
        "enumConstants",
        "enumConstant",
        "interfaceDeclaration",
        "methodDeclaration",
        "genericMethodDeclaration",
        "genericConstructorDeclaration",
        "constructorDeclaration",
        "fieldDeclaration",
        "constDeclaration",
        "constantDeclarator",
        "interfaceMethodDeclaration",
        "genericInterfaceMethodDeclaration",
        "variableDeclarator",
        "variableInitializer",
        "arrayInitializer",
        "typeArgument",
        "qualifiedNameList",
        "receiverParameter",
        "formalParameter",
        "lastFormalParameter",
        "lambdaLVTIParameter",
        "qualifiedName",
        "literal",
        "integerLiteral",
        "floatLiteral",
        "altAnnotationQualifiedName",
        "annotation",
        "elementValuePairs",
        "elementValuePair",
        "elementValue",
        "elementValueArrayInitializer",
        "annotationTypeDeclaration",
        "annotationTypeElementDeclaration",
        "annotationTypeElementRest",
        "annotationMethodOrConstantRest",
        "annotationMethodRest",
        "annotationConstantRest",
        "defaultValue",
        "moduleDeclaration",
        "moduleDirective",
        "requiresModifier",
        "recordDeclaration",
        "recordHeader",
        "recordComponentList",
        "recordComponent",
        "localVariableDeclaration",
        "localTypeDeclaration",
        "statement",
        "catchClause",
        "catchType",
        "finallyBlock",
        "resourceSpecification",
        "resources",
        "resource",
        "switchBlockStatementGroup",
        "switchLabel",
        "forControl",
        "forInit",
        "enhancedForControl",
        "parExpression",
        "methodCall",
        "pattern",
        "lambdaExpression",
        "primary",
        "switchExpression",
        "switchLabeledRule",
        "guardedPattern",
        "switchRuleOutcome",
        "classType",
        "creator",
        "createdName",
        "innerCreator",
        "arrayCreatorRest",
        "classCreatorRest",
        "explicitGenericInvocation",
        "typeArgumentsOrDiamond",
        "nonWildcardTypeArgumentsOrDiamond",
        "nonWildcardTypeArguments",
        "typeArguments",
        "superSuffix",
        "explicitGenericInvocationSuffix",
        "arguments",
    ]

    header = "from JavaParserListener import JavaParserListener\nfrom JavaParser import JavaParser\nfrom node import Node\n\n"

    def _emptyRule(self, rule, lines):
        lines.append(
            f"\tdef enter{rule[0].upper() + rule[1:]}(self, ctx:JavaParser.{rule[0].upper() + rule[1:]}Context):"
        )
        lines.append("\t\tpass\n")
        lines.append(
            f"\tdef exit{rule[0].upper() + rule[1:]}(self, ctx:JavaParser.{rule[0].upper() + rule[1:]}Context):"
        )
        lines.append("\t\tpass\n")

    def _enterRule(self, rule):
        return f"\tdef enter{rule[0].upper() + rule[1:]}(self, ctx:JavaParser.{rule[0].upper() + rule[1:]}Context):"

    def _exitRule(self, rule):
        return f"\tdef exit{rule[0].upper() + rule[1:]}(self, ctx:JavaParser.{rule[0].upper() + rule[1:]}Context):"

    def _generateTreeBuilder(
        self, permittedRuleNames=permittedRuleNames, classname="TreeBuilder"
    ):
        # Generates a treebuilder, which builds a tree with node.Nodes
        if permittedRuleNames == None:
            permittedRuleNames = JavaParser.ruleNames
        lines = [
            f"class {classname}(JavaParserListener):",
            "\tdef __init__(self):",
            "\t\tself.root = None",
            "\t\tself.currentNode = self.root",
            "\n",
        ]
        for rule in JavaParser.ruleNames:
            if rule in permittedRuleNames:
                lines.extend(
                    [
                        self._enterRule(rule),
                        "\t\tif self.root == None:",
                        "\t\t\tnode = Node(ctx)",
                        "\t\t\tself.root = node",
                        "\t\t\tself.currentNode = node",
                        "\t\telse:",
                        "\t\t\tnode = Node(ctx, self.currentNode)",
                        "\t\t\tself.currentNode.addChild(node)",
                        "\t\t\tself.currentNode = node",
                        "\n",
                        self._exitRule(rule),
                        "\t\tif self.currentNode == None:",
                        "\t\t\tself.currentNode = self.root",
                        "\t\telse:",
                        "\t\t\tself.currentNode = self.currentNode.parent",
                        "\n",
                    ]
                )
        return "\n".join(lines)

    def generateTreeBuilder(
        self, filePath, mode="wt", permittedRuleNames=permittedRuleNames
    ):
        with open(filePath, mode) as f:
            if mode == "wt":
                f.write(self.header)
            f.write(self._generateTreeBuilder(permittedRuleNames=permittedRuleNames))

    def _generatePrintListener(
        self, permittedRuleNames=permittedRuleNames, classname="PrintListener", indent=4
    ):
        # Generates a printlistener, which pretty prints the tree structure consisting of only the permitted rules
        if permittedRuleNames == None:
            permittedRuleNames = JavaParser.ruleNames
        lines = [
            f"class {classname}(JavaParserListener):",
            "\tdef __init__(self):",
            "\t\tself.indent = 0",
        ]
        for rule in JavaParser.ruleNames:
            if rule in permittedRuleNames:
                lines.append(self._enterRule(rule))
                lines.append(
                    '\t\tprint(" "*(self.indent-4)+"-"*(0 if self.indent < 4 else 4) + ctx.__class__.__name__)'
                )
                lines.append("\t\tself.indent += 4\n")

                lines.append(self._exitRule(rule))
                lines.append("\t\tself.indent -= 4\n")

        return "\n".join(lines)

    def generatePrintListener(
        self, filePath, mode="wt", permittedRuleNames=permittedRuleNames
    ):

        with open(filePath, mode) as f:
            if mode == "wt":
                f.write(self.header)
            f.write(self._generatePrintListener(permittedRuleNames=permittedRuleNames))

    def generatePrintAllListener(self, filePath, mode="wt"):
        with open(filePath, mode) as f:
            if mode == "wt":
                f.write(self.header)
            f.write(
                self._generatePrintListener(
                    permittedRuleNames=None, classname="PrintAllListener"
                )
            )
