from antlr4 import *
from antlr_parser.JavaLexer import JavaLexer
from antlr_parser.JavaParser import JavaParser
from antlr_parser.JavaParserListener import JavaParserListener
from antlr_parser.JavaParserListenerGenerator import JavaParserListenerGenerator
from antlr_parser.JavaParserVisitor import JavaParserVisitor

import listener
import treebuilder
import node


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


source = """interface Formula {
    double calculate(int a);
    
    default double sqrt(int a) {
        return Math.sqrt(a);
    }
}"""


def main():
    gen = JavaParserListenerGenerator()
    gen.generatePrintListener("listener.py", "wt")
    gen.generatePrintAllListener("listener.py", "at")

    gen.generateTreeBuilder("treebuilder.py", "wt")

    # input = FileStream(file)
    input = InputStream(source)
    lexer = JavaLexer(input)
    stream = CommonTokenStream(lexer)
    parser = JavaParser(stream)
    treeBuilder = treebuilder.TreeBuilder()
    parser.addParseListener(treeBuilder)
    parser.addParseListener(listener.PrintListener())
    tree = parser.compilationUnit()
    root = treeBuilder.root
    root.print()
    binaryNode = node.binarize(root)
    binaryNode.print()
    print(binaryNode.vector(3))

    # print(tree.toStringTree(recog=parser))
    # visitor.Visitor().visitCompilationUnit(tree)
    # generateVisitor()


if __name__ == "__main__":
    main()
