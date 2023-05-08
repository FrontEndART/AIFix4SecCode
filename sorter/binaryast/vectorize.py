from typing import Tuple
import node.treebuilder as treebuilder
import node.node as node
import numpy as np
from pickle import load as pload

from vectorizer import VectorizerStrategy

import antlr_parser.listeners as listeners

from antlr4 import InputStream, CommonTokenStream, ParseTreeWalker
from antlr_parser.JavaLexer import JavaLexer
from antlr_parser.JavaParser import JavaParser


class BinaryASTVectorizer(VectorizerStrategy):

    PCA_PATH = "binaryast/pca"

    def createAST(self, source: str) -> node.Node:
        input = InputStream(source)
        lexer = JavaLexer(input)
        stream = CommonTokenStream(lexer)
        parser = JavaParser(stream)
        treeBuilder = treebuilder.TreeBuilder()
        # parser.addParseListener(treeBuilder)
        # parser.addParseListener(listener.PrintListener())
        tree = parser.compilationUnit()
        ParseTreeWalker.DEFAULT.walk(treeBuilder, tree)
        return treeBuilder.root

    def createBinaryAST(self, source: str) -> node.BinaryNode:
        root = self.createAST(source)
        return node.binarize(root)

    def reduceDimension(self, vector: np.ndarray) -> np.ndarray:
        with open(BinaryASTVectorizer.PCA_PATH, "rb"):
            pca = pload()
        return pca.transform(vector)

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        original_binaryAST = self.createBinaryAST(original)
        patched_binaryAST = self.createBinaryAST(patchedOriginal)
        original_root, patched_root = node.findLowestCommonRoot(
            original_binaryAST, patched_binaryAST
        )
        return self.reduceDimension(original_root.vector(15)), self.reduceDimension(
            patched_root.vector(15)
        )
