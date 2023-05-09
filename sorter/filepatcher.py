import unidiff
import os.path
import binaryast.antlr_parser.JavaLexer as JavaLexer, binaryast.antlr_parser.JavaParser as JavaParser
from antlr4 import InputStream, CommonTokenStream
from antlr4.Token import Token
from antlr4.tree.Tree import Tree
from io import StringIO
from binaryast.antlr_parser.MethodContextExtractorVisitor import (
    MethodContextExtractorVisitor,
)


class FilePatcher:
    """Reads patchfiles and extracts the original or patched version of the source code"""

    def __init__(self, basepath="", mode="method"):
        # makes the dictionary of the existing implementations
        # the implementations name can be used as a lowercase name
        # with or without ending in 'filepatcher'
        implementations = dict()
        for subclass in FilePatcherStrategy.__subclasses__():
            implementations[subclass.__name__.lower()[: -len("filepatcher")]] = subclass
            implementations[subclass.__name__.lower()] = subclass

        # if the implementation doesn't exist the methodfilepatcher will be used as default
        if mode.lower() in implementations:
            self.filepatcher = implementations[mode.lower()](basepath)
        else:
            self.filepatcher = MethodFilePatcher(basepath)

    def extractOriginal(self, patchPath: str) -> str:
        """Extracts the original source code with the specified filepatcher strategy

        Parameters
        ----------
        patchPath : str
            Path to the patchfile

        Returns
        -------
        str
            The source code
        """
        return self.filepatcher.extractOriginal(patchPath)

    def patchFile(self, patchPath: str) -> str:
        """Extracts the patched source code with the specified filepatcher strategy

        Parameters
        ----------
        patchPath : str
            Path to the patchfile

        Returns
        -------
        str
            The patched souce code
        """
        return self.filepatcher.patchFile(patchPath)


class FilePatcherStrategy:
    """Inteface for filepatcher implementations"""

    def __init__(self, basepath=""):
        self.basepath = basepath

    def extractOriginal(self, patchPath: str) -> str:
        raise NotImplementedError()

    def patchFile(self, patchPath: str) -> str:
        raise NotImplementedError()


class FullFilePatcher(FilePatcherStrategy):
    """Reads patchfiles and extracts the original or patched version of the source code.
    The path to the original file is also extracted from the patchfile, and the full file is patched not just the context."""

    def __init__(self, basepath=""):
        super().__init__(basepath)
        self.patchSet = None
        self.patchPath = None

    def _readPatchSet(self, patchPath: str):
        """Reads the diff file and patchset, if it hasn't been read yet.

        Parameters
        ----------
        patchPath : str
            The path to the patch (diff) file
        """
        if self.patchSet is None or self.patchPath != patchPath:
            self.patchSet = unidiff.PatchSet.from_filename(patchPath)
            self.patchPath = patchPath

    def patchFileLines(self, patchPath: str) -> list[str]:
        """Extracts the patched version of the source code based on the patch file and the original source

        Parameters
        ----------
        patchPath : str
            The path to the patch file

        Returns
        -------
        str
            The lines of the patched source code
        """
        patchPath = os.path.join(self.basepath, patchPath)
        self._readPatchSet(patchPath)
        originalFilePath = os.path.join(
            self.basepath,
            self.patchSet[0].source_file,
        )
        with open(originalFilePath, "rt") as file:
            originalLines = file.readlines()
        patchedLines = []
        lastStart = 0
        # add every row which is context or in the patched version
        for hunk in self.patchSet[0]:
            patchedLines.extend(originalLines[lastStart : hunk.source_start - 1])
            lastStart = hunk.source_start - 1 + hunk.source_length
            for line in hunk.target:
                line = self._cleanLine(line)
                patchedLines.append(line)
        # If the last line didn't have a new line character, then add one
        if lastStart < len(originalLines) and not patchedLines[-1].endswith(
            ("\n\r", "\n", "\r")
        ):
            patchedLines[-1] += "\n"
        # add remaining lines
        patchedLines.extend(originalLines[lastStart:])
        return patchedLines

    def _cleanLine(self, line: str) -> str:
        """Removes the first character ("+", "-", " ") of a line in the patchfile.
        If there was originally a tab, then it is restored.

        Parameters
        ----------
        line : str
            A source code line in the patchfile

        Returns
        -------
        str
            The cleaned line
        """
        if line.startswith(("+", "-")):
            if len(line) == 1:
                return "\n"
            elif line[1:].startswith("   "):
                return "\t" + line[4:]
            elif line[1] == " ":
                return " " + line[1:]
            else:
                return line[1:]
        if line.startswith(" "):
            if line[1:3] == "   ":
                return "\t" + line[4:]
            else:
                return line
        return line

    def patchFile(self, patchPath: str) -> str:
        """Extracts the patched version of the source code based on the patch file and the original source

        Parameters
        ----------
        patchPath : str
            The path to the patch file

        Returns
        -------
        str
            The patched source code
        """
        return "".join(self.patchFileLines(patchPath))

    def extractOriginal(self, patchPath: str) -> str:
        """Extracts the full original source file

        Parameters
        ----------
        patchPath : str
            The path to the patch file

        Returns
        -------
        str
            The full original source code
        """
        patchPath = os.path.join(self.basepath, patchPath)
        self._readPatchSet(patchPath)
        originalFilePath = os.path.join(
            self.basepath,
            self.patchSet[0].source_file,
        )
        with open(originalFilePath, "rt") as file:
            lines = file.readlines()
        return "".join(lines)


class PartialFilePatcher(FilePatcherStrategy):
    """Reads patchfiles and extracts the original or patched version of the source code.
    Only the context and the changed lines are in the extracted original and patched source codes."""

    def filterLines(
        self,
        lines: list[str],
        lineBeginnings: list[str] = ["+"],
        badLineBeginnings: list[str] = ["@@", "---", "+++", "===", "-", "Index"],
    ) -> list[str]:
        """Filters the lines based on their starting characters

        Parameters
        ----------
        lines : list[str]
            The lines to be filtered
        lineBeginnings : list[str], optional
            Lines beginning with these should be kept, and the starting characters be changed to a space, by default ["+"]
        badLineBeginnings : list[str], optional
            Lines beginning with these should be removed, by default ["@@", "---", "+++", "===", "-", "Index"]

        Returns
        -------
        list[str]
            The filtered lines
        """
        filteredLines = []
        for line in lines:
            if not line.startswith((*badLineBeginnings,)):
                for beginning in lineBeginnings:
                    if line.startswith(beginning):
                        line = " " * len(beginning) + line[len(beginning) :]
                        break
                filteredLines.append(line)

        return filteredLines

    def extractOriginal(self, patchPath: str) -> str:
        """Extracts the original source code based on only the patchfile

        Parameters
        ----------
        patchPath : str
            Path to the patch (e.g. .diff) file

        Returns
        -------
        str
            The original source code
        """
        with open(os.path.join(self.basepath, patchPath), "rt") as file:
            lines = file.readlines()

        return "".join(
            self.filterLines(
                lines, ["-", " "], ["@@", "---", "+++", "===", "+", "Index"]
            )
        )

    def patchFile(self, patchPath: str) -> str:
        """Extracts the patched version of the source code based on only the patchfile

        Parameters
        ----------
        patchPath : str
            Path to the patch (e.g. .diff) file

        Returns
        -------
        str
            The patched source code
        """

        with open(os.path.join(self.basepath, patchPath), "rt") as file:
            lines = file.readlines()

        return "".join(
            self.filterLines(
                lines, ["+", " "], ["@@", "---", "+++", "===", "-", "Index"]
            )
        )


class MethodFilePatcher(FilePatcherStrategy):
    def __init__(self, basepath=""):
        self.basepath = basepath
        self.patchSet = None
        self.patchPath = None
        self.fullFilePatcher = FullFilePatcher(basepath)

    def _readPatchSet(self, patchPath: str):
        """Reads the diff file and patchset, if it hasn't been read yet.

        Parameters
        ----------
        patchPath : str
            The path to the patch (diff) file
        """
        if self.patchSet is None or self.patchPath != patchPath:
            self.patchSet = unidiff.PatchSet.from_filename(patchPath)
            self.patchPath = patchPath

    def _createParseTree(
        self, source: str
    ) -> JavaParser.JavaParser.CompilationUnitContext:
        """Creates a parse tree from the source code
        ANTLR parser is used and a few types node types are filtered out in order to make the tree shallower
        Parameters
        ----------
        source : str
            The source code

        Returns
        -------
        JavaParser.JavaParser.CompilationUnitContext
            The root node of the AST
        """
        input = InputStream(source)
        lexer = JavaLexer.JavaLexer(input)
        stream = CommonTokenStream(lexer)
        parser = JavaParser.JavaParser(stream)
        tree = parser.compilationUnit()
        return tree

    def _getSourceCodeFromTree(self, t: Tree) -> str:
        """Constucts the source code from an ANTLR parse tree

        Parameters
        ----------
        t : Tree
            The ANTLR parse tree

        Returns
        -------
        str
            The source code
        """
        # If the node has no children, then the payload is converted into text
        if t.getChildCount() == 0:
            payload = t.getPayload()
            if isinstance(payload, Token):
                return payload.text
            return str(t.getPayload())
        # If the node has children, all the subtrees' text is written out recursively, with a space in between
        with StringIO() as buf:
            for index, child in enumerate(t.getChildren()):
                if index > 0:
                    buf.write(" ")
                buf.write(self._getSourceCodeFromTree(child))
            return buf.getvalue()

    def _contextToLines(self, fullLines: list[str], ctxs) -> list[str]:
        """Filters the lines based on the contexts. Only those lines are kept, which are in the contexts.

        Parameters
        ----------
        fullLines : list[str]
            List of lines
        ctxs :
            List of contexts

        Returns
        -------
        list[str]
            The fitered lines
        """
        methodlines = []
        if ctxs is not None:
            for ctx in ctxs:
                methodlines.extend(fullLines[ctx.start.line - 1 : ctx.stop.line + 1])
        return methodlines

    def extractOriginal(self, patchPath: str) -> str:
        """Extracts the methods from the original source code, where a modification is done in the patch

        Parameters
        ----------
        patchPath : str
            The path to the patch file

        Returns
        -------
        str
            The methods of the original source code
        """
        patchPath = os.path.join(self.basepath, patchPath)
        self._readPatchSet(patchPath)
        originalFilePath = os.path.join(
            self.basepath,
            self.patchSet[0].source_file,
        )

        # all the line numbers are gathered from the hunks
        linenumbers = []
        for hunk in self.patchSet[0]:
            linenumbers.extend(
                list(range(hunk.source_start, hunk.source_start + hunk.source_length))
            )

        with open(originalFilePath, "rt") as file:
            lines = file.readlines()

        tree = self._createParseTree("".join(lines))
        methodcontexts = tree.accept(MethodContextExtractorVisitor(linenumbers))

        return "".join(self._contextToLines(lines, methodcontexts))

    def patchFile(self, patchPath: str) -> str:
        """Extracts the methods from the patched source code, where a modification is done in the patch

        Parameters
        ----------
        patchPath : str
            The path to the patch file

        Returns
        -------
        str
            The methods of the patched source code
        """
        patchedLines = self.fullFilePatcher.patchFileLines(patchPath)
        linenumbers = []
        # all the line numbers are gathered from the hunks
        for hunk in self.patchSet[0]:
            linenumbers.extend(
                list(range(hunk.target_start, hunk.target_start + hunk.target_length))
            )
        tree = self._createParseTree("".join(patchedLines))
        methodcontexts = tree.accept(MethodContextExtractorVisitor(linenumbers))
        return "".join(self._contextToLines(patchedLines, methodcontexts))
