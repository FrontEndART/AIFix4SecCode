import itertools
from typing import Tuple
import numpy as np
from binaryast.antlr_parser.JavaParser import JavaParser


def name_to_number(name: str):
    #Converts the context name into a number
    contexts = [(rule.lower() + "context") for rule in JavaParser.ruleNames]
    try:
        return contexts.index(name.lower()) + 1
    except ValueError as _:
        return 0


class Node:
    """A simple node, with which trees can be build"""
    def __init__(self, originalObject=None, parent=None):
        self.originalObject = originalObject
        self.name = originalObject.__class__.__name__
        self.parent = parent
        self.children = []

    def addChild(self, child):
        self.children.append(child)

    def print(self, indent=0):
        print(" " * indent, self.name)
        for child in self.children:
            child.print(indent=indent + 4)


class BinaryNode:
    """A simple binary node"""
    def __init__(self, originalObject):
        self.originalObject = originalObject
        self.name = originalObject.__class__.__name__
        self.leftChild = NullBinaryNode()
        self.rightChild = NullBinaryNode()

    def print(self, indent=0):
        print(" " * indent, self.name)
        self.leftChild.print(indent + 4)
        self.rightChild.print(indent + 4)

    def flatten(self, index, depth, vector):
        if index >= 2**depth:
            return
        vector[index] = name_to_number(self.name)
        self.leftChild.flatten(2 * index + 1, depth, vector)
        self.rightChild.flatten(2 * index + 2, depth, vector)

    def vector(self, depth):
        vector = np.zeros(2**depth, dtype=np.int8)
        self.flatten(0, depth, vector)
        return vector


class NullBinaryNode(BinaryNode):
    """Null binary node"""
    def __init__(self, originalObject=None):
        self.originalObject = None
        self.name = "Null"

    def print(self, indent=0):
        print(" " * indent, self.name)

    def flatten(self, index, depth, vector):
        if index >= 2**depth:
            return
        vector[index] = name_to_number(self.name)


def binarize(tree: Node) -> BinaryNode:
    """Creates a binary tree from a tree.
        The children go to the right, the siblings go to the left."""

    binaryNode = BinaryNode(tree.originalObject)
    if tree.children == None or len(tree.children) == 0:
        return binaryNode

    binaryNode.rightChild = binarize(tree.children[0])

    currentNode = binaryNode.rightChild
    for child in itertools.islice(tree.children, 1, None):
        currentNode.leftChild = binarize(child)
        currentNode = currentNode.leftChild
    return binaryNode


def findLowestCommonRoot(a: BinaryNode, b: BinaryNode) -> Tuple[BinaryNode, BinaryNode]:
    """Finds the lowest common root of two binary trees.
        (If the two trees are identical, then the lowest common root will be the original root of the trees."""
    def _findLowestCommonRoot(
        a: BinaryNode, b: BinaryNode
    ) -> Tuple[BinaryNode, BinaryNode, bool]:
        if a.name != b.name:
            return a, b, False
        # leaf node
        if (
            a.leftChild.name == "Null"
            and a.rightChild.name == "Null"
            and b.leftChild.name == "Null"
            and b.rightChild.name == "Null"
        ):
            return a, b, True
        # left or right children differ
        if (
            a.leftChild.name != b.leftChild.name
            or a.rightChild.name != b.rightChild.name
        ):
            return a, b, False
        # the left and right children are the same
        # check the subtrees
        if (
            a.leftChild.name == b.leftChild.name
            and a.rightChild.name == b.rightChild.name
        ):
            # one of the nodes is a null node (null nodes have no children)
            if a.leftChild.name == "Null":
                return _findLowestCommonRoot(a.rightChild, b.rightChild)
            elif a.rightChild.name == "Null":
                return _findLowestCommonRoot(a.leftChild, b.leftChild)

            left = _findLowestCommonRoot(a.leftChild, b.leftChild)
            a_left, b_left, left_identical = left
            right = _findLowestCommonRoot(a.rightChild, b.rightChild)
            a_right, b_right, right_identical = right

            if left_identical and right_identical:
                return a, b, True
            elif left_identical:
                return a_right, b_right, False
            elif right_identical:
                return a_left, b_left, False
            else:
                return a, b, False

    a_result, b_result, identical = _findLowestCommonRoot(a, b)
    return a_result, b_result
