import JavaParser


def generateVisitor():
    with open("visitor.py", "wt") as file:
        file.write("from JavaParserVisitor import JavaParserVisitor\n")
        file.write("from JavaParser import JavaParser\n")
        file.write("class Visitor(JavaParserVisitor):\n\n")
        for rule in JavaParser.ruleNames:
            file.write(
                f"\tdef visit{rule[0].upper() + rule[1:]}(self, ctx: JavaParser.{rule[0].upper() + rule[1:]}Context):\n"
            )
            file.write("\t\tprint(ctx.__class__.__name__)\n")
            file.write("\t\treturn self.visitChildren(ctx)\n\n")
