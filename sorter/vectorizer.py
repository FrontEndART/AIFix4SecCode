from typing import Iterable, Tuple
from gensim.models.keyedvectors import KeyedVectors
from gensim.models.doc2vec import Doc2Vec
import javalang
import numpy as np
import re
import binaryast.node.treebuilder as treebuilder
import binaryast.node.node as node
from pickle import load as pload
from antlr4 import InputStream, CommonTokenStream, ParseTreeWalker
from binaryast.antlr_parser.JavaLexer import JavaLexer
from binaryast.antlr_parser.JavaParser import JavaParser
from pathlib import Path
import os


class Vectorizer:
    """Vectorizes source codes with several algorithms"""

    def __init__(self, vectorizer_alg: str):
        """Chooses the vectorizer implementation based on its name

        Parameters
        ----------
        vectorizer_alg : str
            Name of the vectorizer algorithm
        """
        # makes the dictionary of the existing implementations
        # the implementations name can be used as a lowercase name
        # with or without ending in 'vectorizer'/'vectorizerstrategy'
        implementations = dict()
        for subclass in VectorizerStrategy.__subclasses__():
            implementations[subclass.__name__.lower()[: -len("vectorizer")]] = subclass
            implementations[
                subclass.__name__.lower()[: -len("vectorizerstrategy")]
            ] = subclass
            implementations[subclass.__name__.lower()] = subclass

        # if the implementation doesn't exist, then the bag of words will be used as default
        if vectorizer_alg.lower() in implementations:
            self.vectorizer = implementations[vectorizer_alg.lower()]()
        else:
            self.vectorizer = BagOfWordsVectorizer()

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        return self.vectorizer.vectorize(original, patchedOriginal)

    def bulkVectorize(
        self, original: str, patchedSources: Iterable[str]
    ) -> Tuple[np.ndarray, list[np.ndarray]]:
        """Vectorizes the original source code and all the patch candidates for the same source code

        Parameters
        ----------
        original : str
            The original source code
        patchedSources : Iterable[str]
            The candidate patched source codes for the same original code snippet

        Returns
        -------
        Tuple[np.ndarray, list[np.ndarray]]
            A tuple containing the vectors representing the original and patched candidate source codes
        """
        return self.vectorizer.bulkVectorize(original, patchedSources)


class VectorizerStrategy:
    """A source code vectorizer implementation"""

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        raise NotImplementedError()

    def bulkVectorize(
        self, original: str, patchedSources: Iterable[str]
    ) -> Tuple[np.ndarray, list[np.ndarray]]:
        """Vectorizes the original source code and all the patch candidates for the same source code

        Parameters
        ----------
        original : str
            The original source code
        patchedSources : Iterable[str]
            The candidate patched source codes for the same original code snippet

        Returns
        -------
        Tuple[np.ndarray, list[np.ndarray]]
            A tuple containing the vectors representing the original and patched candidate source codes
        """
        result = []
        for patched in patchedSources:
            vectorizedOriginal, vectorizedPatched = self.vectorize(original, patched)
            result.append(vectorizedPatched)
        return vectorizedOriginal, result


class CharacterVectorizer(VectorizerStrategy):
    """Splits the code into characters and converts the characters into character codes.
    Good for calculating hamming distance on a character level.
    """

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        return (
            np.array([ord(char) for char in original]),
            np.array([ord(char) for char in patchedOriginal]),
        )


def tokenize(source: str) -> list[str]:
    """Tokenizes the string source code into string tokens using the javalang library.
    Keyword, Operator, Separator, Modifier, BasicType tokens are represented with the values,
    the other token types are just represented with the token name.

    Parameters
    ----------
    source : list[str]
        The source code

    Returns
    -------
    list[str]
        The tokenized source code
    """
    tokens = list(javalang.tokenizer.tokenize(source))
    tokenized = []

    tokens_of_interest = {
        "Keyword",
        "Operator",
        "Separator",
        "Modifier",
        "BasicType",
    }

    for token in tokens:
        token_name = token.__class__.__name__
        token_val = token.value

        if token_name in tokens_of_interest:
            tokenized.append(token_name + "_" + token_val)
        else:
            tokenized.append(token_name)

    return tokenized


class TokenVectorizer(VectorizerStrategy):
    """Tokenizes the source code and converting the tokens into numbers."""

    # Dictionary to convert the tokens into a number
    TOKENS_TO_NUMBER = {
        "Identifier": 1,
        "Separator_.": 2,
        "Separator_(": 3,
        "Separator_)": 4,
        "Separator_;": 5,
        "Separator_,": 6,
        "Separator_{": 7,
        "Separator_}": 8,
        "Operator_=": 9,
        "String": 10,
        "DecimalInteger": 11,
        "Modifier_public": 12,
        "Keyword_import": 13,
        "Keyword_return": 14,
        "Keyword_new": 15,
        "Keyword_if": 16,
        "Operator_+": 17,
        "BasicType_int": 18,
        "Null": 19,
        "Separator_[": 20,
        "Separator_]": 21,
        "Keyword_void": 22,
        "Operator_<": 23,
        "Modifier_final": 24,
        "Operator_>": 25,
        "Keyword_this": 26,
        "Modifier_private": 27,
        "Modifier_static": 28,
        "Boolean": 29,
        "Annotation": 30,
        "Operator_==": 31,
        "Keyword_class": 32,
        "Operator_: ": 33,
        "Operator_!=": 34,
        "Keyword_throws": 35,
        "BasicType_boolean": 36,
        "Keyword_else": 37,
        "Operator_-": 38,
        "HexInteger": 39,
        "Keyword_for": 40,
        "Modifier_protected": 41,
        "Keyword_package": 42,
        "Keyword_case": 43,
        "DecimalFloatingPoint": 44,
        "Operator_&&": 45,
        "Operator_!": 46,
        "Keyword_catch": 47,
        "Keyword_try": 48,
        "Operator_++": 49,
        "Keyword_throw": 50,
        "Keyword_extends": 51,
        "Operator_*": 52,
        "BasicType_long": 53,
        "Keyword_super": 54,
        "Operator_?": 55,
        "BasicType_byte": 56,
        "Keyword_instanceof": 57,
        "Operator_||": 58,
        "Keyword_break": 59,
        "BasicType_double": 60,
        "BasicType_float": 61,
        "Keyword_implements": 62,
        "Keyword_while": 63,
        "Operator_+=": 64,
        "Operator_/": 65,
        "Operator_>=": 66,
        "Operator_&": 67,
        "BasicType_char": 68,
        "Keyword_finally": 69,
        "Modifier_abstract": 70,
        "Keyword_switch": 71,
        "Operator_<=": 72,
        "Modifier_synchronized": 73,
        "Keyword_interface": 74,
        "Operator_--": 75,
        "BasicType_short": 76,
        "Operator_|": 77,
        "Keyword_continue": 78,
        "Modifier_default": 79,
        "Operator_<<": 80,
        "Keyword_assert": 81,
        "Operator_...": 82,
        "Operator_%": 83,
        "Modifier_native": 84,
        "Operator_|=": 85,
        "Operator_-=": 86,
        "Operator_^": 87,
        "Keyword_enum": 88,
        "Keyword_do": 89,
        "Modifier_transient": 90,
        "HexFloatingPoint": 91,
        "Operator_*=": 92,
        "Modifier_volatile": 93,
        "Operator_~": 94,
        "Operator_&=": 95,
        "OctalInteger": 96,
        "Operator_^=": 97,
        "Operator_/=": 98,
        "Operator_<<=": 99,
        "Operator_>>=": 100,
        "Operator_>>>=": 101,
        "Operator_->": 102,
        "Operator_%=": 103,
        "Keyword_goto": 104,
        "Operator_: : ": 105,
        "BinaryInteger": 106,
        "Modifier_strictfp": 107,
        "Keyword_const": 108,
    }

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        tokenized = tokenize(original)
        tokenized_patched = tokenize(patchedOriginal)
        return (
            np.array(
                [TokenVectorizer.TOKENS_TO_NUMBER.get(token, 0) for token in tokenized]
            ),
            np.array(
                [
                    TokenVectorizer.TOKENS_TO_NUMBER.get(token, 0)
                    for token in tokenized_patched
                ]
            ),
        )


class Word2VecVectorizer(VectorizerStrategy):
    """Vectorizes the source code using the word2vec model.
    The model has to be trained on a tokenized source code.
    The vector for the source code is calculated from the average of all the token embeddings."""

    WORD2VEC_KEYEDVECTORS_PATH = "word2vec/gensim-word2vec-java-corpus-wv"

    def __init__(self):
        """Loads the saved key-vector dictionary which has been already trained"""
        source_path = Path(__file__).resolve()
        source_dir = source_path.parent

        self.wv = KeyedVectors.load(
            os.path.join(source_dir, Word2VecVectorizer.WORD2VEC_KEYEDVECTORS_PATH),
            mmap="r",
        )

    def embedd(self, tokens: Iterable[str]) -> np.ndarray:
        """Calculates the average of the vectors of all the tokens in the tokenized source code

        Parameters
        ----------
        tokens : Iterable[str]
            Token stream representing a source code

        Returns
        -------
        np.ndarray
            The embedding of the source code, obtained by averaging the word vectors of the tokens
        """
        if len(tokens) == 0:
            return np.ndarray(self.wv.vector_size)
        sample_vec = np.zeros(self.wv[tokens[0]].shape)
        number_of_vectors = 0

        for token in tokens:
            if token in self.wv:
                sample_vec += self.wv[token]
                number_of_vectors += 1

        return np.true_divide(sample_vec, number_of_vectors)

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        return (
            self.embedd(tokenize(original)),
            self.embedd(tokenize(patchedOriginal)),
        )


class Doc2VecVectorizer(VectorizerStrategy):
    """Vectorizes the source code using the doc2vec model."""

    DOC2VEC_MODEL_PATH = "doc2vec/gensim-doc2vec-java-corpus"

    def __init__(self):
        """Loads the already trained doc2vec model"""
        source_path = Path(__file__).resolve()
        source_dir = source_path.parent
        self.model = Doc2Vec.load(
            os.path.join(source_dir, Doc2VecVectorizer.DOC2VEC_MODEL_PATH)
        )

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        return (
            self.model.infer_vector(tokenize(original)),
            self.model.infer_vector(tokenize(patchedOriginal)),
        )


class GloVeVectorizer(VectorizerStrategy):
    """Vectorizes the source code using the GloVe model.
    The model has to be trained on a tokenized source code.
    The vector for the source code is calculated from the average of all the token embeddings."""

    GLOVE_VECTORS_PATH = "glove/glove-java-corpus"

    def __init__(self):
        """Loads the trained glove vectors"""
        source_path = Path(__file__).resolve()
        source_dir = source_path.parent
        self.embeddings = dict()
        with open(
            os.path.join(source_dir, GloVeVectorizer.GLOVE_VECTORS_PATH), "rt"
        ) as file:
            for line in file:
                values = line.split()
                self.embeddings[values[0]] = np.asarray(values[1:], "float16")

    def embedd(self, tokens: Iterable[str]) -> np.ndarray:
        """Calculates the average of the vectors of all the tokens in the tokenized source code

        Parameters
        ----------
        tokens : Iterable[str]
            Token stream representing a source code

        Returns
        -------
        np.ndarray
            The embedding of the source code, obtained by averaging the word vectors of the tokens
        """
        if len(tokens) == 0:
            return np.ndarray(list(self.embeddings.values())[0].shape)
        sample_vec = np.zeros(self.embeddings[tokens[0]].shape)
        number_of_vectors = 0

        for token in tokens:
            if token in self.embeddings:
                sample_vec += self.embeddings[token]
                number_of_vectors += 1

        return np.true_divide(sample_vec, number_of_vectors)

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        return (
            self.embedd(tokenize(original)),
            self.embedd(tokenize(patchedOriginal)),
        )


class BagOfWordsVectorizer(VectorizerStrategy):
    """Vectorizes the source code using the Bag of words model.
    The common vocabulary is calculated from both of the vectors (original and patched).
    """

    REGEX = r"\s+|;|,|\.|\(|\)"

    def tokenize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[list[str], list[str], list[str]]:
        """Splits the source code on whitespace and on ";", ".", ",", "(", ")", characters

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[list[str], list[str], list[str]]
            A tuple of the tokens, the tokenized original and the tokenized patched source codes
        """
        # regex = r"\s+|;|\(|\)|\."
        tokenizedPatched = list(
            filter(
                lambda x: x != "",
                re.split(BagOfWordsVectorizer.REGEX, patchedOriginal.lower()),
            )
        )
        tokenizedOriginal = list(
            filter(
                lambda x: x != "",
                re.split(BagOfWordsVectorizer.REGEX, original.lower()),
            )
        )
        tokens = set()
        tokens.update(tokenizedOriginal)
        tokens.update(tokenizedPatched)
        tokens = sorted(tokens)

        return tokens, tokenizedOriginal, tokenizedPatched

    def tokenizeSingle(self, source: str) -> list[str]:
        """Tokenizes a source code snippet

        Parameters
        ----------
        source : str
            The source code snippet

        Returns
        -------
        list[str]
            The tokenized source code
        """
        return list(
            filter(
                lambda x: x != "", re.split(BagOfWordsVectorizer.REGEX, source.lower())
            )
        )

    def createVector(self, tokens: list[str], tokenized: list[str]) -> np.ndarray:
        """Counts the occurence of each token in the tokenized source code

        Parameters
        ----------
        tokens : list[str]
            The token dictionary, all the possible tokens
        tokenized : list[str]
            The tokenized source code

        Returns
        -------
        np.ndarray
            A vector, in which the number in each position is the count of the token corresponding to that position.
            The order of the tokens is the same as in the given tokens parameter
        """
        counter = dict()
        vector = np.zeros((len(tokens)))
        for token in tokenized:
            counter[token] = counter.get(token, 0) + 1

        for token, count in counter.items():
            index = tokens.index(token)
            vector[index] = count
        return vector

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        tokens, tokenizedOriginal, tokenizedPatched = self.tokenize(
            original, patchedOriginal
        )
        return (
            self.createVector(tokens, tokenizedOriginal),
            self.createVector(tokens, tokenizedPatched),
        )

    def bulkVectorize(
        self, original: str, patchedSources: Iterable[str]
    ) -> Tuple[np.ndarray, list[np.ndarray]]:
        """Vectorizes the original source code and all the patch candidates for the same source code
        The bag-of-words dictionary will be the same for all of the vectors

        Parameters
        ----------
        original : str
            The original source code
        patchedSources : Iterable[str]
            The candidate patched source codes for the same original code snippet

        Returns
        -------
        Tuple[np.ndarray, list[np.ndarray]]
            A tuple containing the vectors representing the original and patched candidate source codes
        """
        tokens = set()
        tokenizedOriginal = self.tokenizeSingle(original)
        tokenized = [self.tokenizeSingle(patch) for patch in patchedSources]
        tokens.update(tokenizedOriginal)
        tokens.update(*tokenized)
        tokens = sorted(tokens)
        vectorizedOriginal = self.createVector(tokens, tokenizedOriginal)
        vectorizedPatched = [
            self.createVector(tokens, patch) for patch in patchedSources
        ]
        return vectorizedOriginal, vectorizedPatched


class BinaryASTVectorizer(VectorizerStrategy):
    """Vectorizes the source code with the use of its AST. The AST is parsed with the help
    of ANTLR. Then the AST is binarized, encoded into a fix-length vector, then the
    dimensions of the fix-length vector is reduced with a pre-fitted PCA."""

    PCA_PATH = "binaryast/pca"

    def createAST(self, source: str) -> node.Node:
        """Creates an AST from the source code
        ANTLR parser is used and a few a nodes are filtered out in order to make the tree shallower
        Parameters
        ----------
        source : str
            The source code

        Returns
        -------
        node.Node
            The root node of the AST
        """
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
        """Creates a binary AST of the source code

        Parameters
        ----------
        source : str
            The source code

        Returns
        -------
        node.BinaryNode
            The root of the binary AST
        """
        root = self.createAST(source)
        return node.binarize(root)

    def reduceDimension(self, vector: np.ndarray) -> np.ndarray:
        """Reduces the dimension of the vectorized binary AST with PCA

        Parameters
        ----------
        vector : np.ndarray
            The vector representing the binary AST

        Returns
        -------
        np.ndarray

        """
        source_path = Path(__file__).resolve()
        source_dir = source_path.parent
        with open(os.path.join(source_dir, BinaryASTVectorizer.PCA_PATH), "rb") as file:
            pca = pload(file)
        return pca.transform(vector.reshape(1, -1))[0]

    def vectorize(
        self, original: str, patchedOriginal: str
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Vectorizes the original source code and a patched source code

        Parameters
        ----------
        original : str
            The original source code
        patchedOriginal : str
            The patched source code

        Returns
        -------
        Tuple[np.ndarray, np.ndarray]
            A tuple containing the vectors representing the original and patched source code
        """
        original_binaryAST = self.createBinaryAST(original)
        patched_binaryAST = self.createBinaryAST(patchedOriginal)
        original_root, patched_root = node.findLowestCommonRoot(
            original_binaryAST, patched_binaryAST
        )
        return (
            self.reduceDimension(original_root.vector(15)),
            self.reduceDimension(patched_root.vector(15)),
        )
