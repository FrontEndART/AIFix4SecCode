from traceback import TracebackException
import javalang
from pprint import pprint
import itertools
import numpy as np


class Node:
    def __init__(self, originalASTNode):
        self.originalASTNode = originalASTNode
        self.name = originalASTNode.__class__.__name__
        self.children = []

    def addChild(self, child):
        self.children.append(child)


class BinaryNode:
    def __init__(self, originalNode):
        self.originalNode = originalNode
        self.name = originalNode.name
        self.leftChild = NullBinaryNode()
        self.rightChild = NullBinaryNode()


class NullBinaryNode(BinaryNode):
    def __init__(self, originalNode=None):
        self.originalNode = None
        self.name = "Null"


def makeTree(tree):
    node = Node(tree)
    if isinstance(tree, javalang.ast.Node):
        for item in tree.children:
            if isinstance(item, list):
                for child in item:
                    node.addChild(makeTree(child))
    return node


def printTree(node, indent=0):
    print(" " * indent, node.name)
    for child in node.children:
        printTree(child, indent=indent + 4)


def printBinTree(node, indent=0):
    print(" " * indent, node.name)
    if not (isinstance(node, NullBinaryNode)):
        printBinTree(node.leftChild, indent + 4)
        printBinTree(node.rightChild, indent + 4)


def binarize(node):
    binaryNode = BinaryNode(node)
    if len(node.children) == 0:
        return binaryNode
    binaryNode.rightChild = binarize(node.children[0])

    currentNode = binaryNode.rightChild  # BinaryNode(node.children[-1])
    for child in itertools.islice(node.children, 1, None):
        currentNode.leftChild = binarize(child)
        currentNode = currentNode.leftChild
    return binaryNode


def name_to_index(name):
    name_to_index_dict = {
        "Null": 0,
        "MethodInvocation": 1,
        "ReferenceType": 2,
        "Literal": 3,
        "StatementExpression": 4,
        "BinaryOperation": 5,
        "VariableDeclarator": 6,
        "FormalParameter": 7,
        "MethodDeclaration": 8,
        "BasicType": 9,
        "LocalVariableDeclaration": 10,
        "Import": 11,
        "BlockStatement": 12,
        "Assignment": 13,
        "ReturnStatement": 14,
        "IfStatement": 15,
        "ClassCreator": 16,
        "FieldDeclaration": 17,
        "TypeArgument": 18,
        "This": 19,
        "Annotation": 20,
        "Cast": 21,
        "ArraySelector": 22,
        "ClassDeclaration": 23,
        "ForStatement": 24,
        "CompilationUnit": 25,
        "PackageDeclaration": 26,
        "VariableDeclaration": 27,
        "ClassReference": 28,
        "ConstructorDeclaration": 29,
        "CatchClauseParameter": 30,
        "CatchClause": 31,
        "TryStatement": 32,
        "SwitchStatementCase": 33,
        "ThrowStatement": 34,
        "ArrayCreator": 35,
        "SuperConstructorInvocation": 36,
        "ArrayInitializer": 37,
        "ForControl": 38,
        "BreakStatement": 39,
        "ElementValuePair": 40,
        "EnhancedForControl": 41,
        "TernaryExpression": 42,
        "ConstantDeclaration": 43,
        "WhileStatement": 44,
        "EnumConstantDeclaration": 45,
        "SwitchStatement": 46,
        "TypeParameter": 47,
        "InterfaceDeclaration": 48,
        "ContinueStatement": 49,
        "ExplicitConstructorInvocation": 50,
        "SynchronizedStatement": 51,
        "ElementArrayValue": 52,
        "AssertStatement": 53,
        "EnumDeclaration": 54,
        "EnumBody": 55,
        "DoStatement": 56,
        "Statement": 57,
        "AnnotationMethod": 58,
        "SuperMemberReference": 59,
        "AnnotationDeclaration": 60,
        "InnerClassCreator": 61,
        "VoidClassReference": 62,
        "TryResource": 63,
        "LambdaExpression": 64,
        "InferredFormalParameter": 65,
        "MethodReference": 66,
        "MemberReference": 67,
    }
    return name_to_index_dict[name] if name in name_to_index_dict else 100


def vectorize(binaryNode, depth):
    vector = np.zeros(2**depth, dtype=np.int8)

    def _flatten(binaryNode, index):
        if index >= 2**depth:
            return
        if not isinstance(binaryNode, NullBinaryNode):
            vector[index] = name_to_index(binaryNode.name)
            _flatten(binaryNode.leftChild, 2 * index + 1)
            _flatten(binaryNode.rightChild, 2 * index + 2)

    _flatten(binaryNode, 0)
    return vector


def main():
    source = r"""class HelloWorld {
        public static void main(String[] args) {
            System.out.println("Hello, World!"); 
        }

        public static void fun(){
            int a = 100;
        }
    }
    class Test {}"""

    tree = javalang.parse.parse(source)

    node = makeTree(tree)
    printTree(node)

    printBinTree(binarize(node))

    print(vectorize(binarize(node), 20))


if __name__ == "__main__":
    main()
