from contextvars import Context
from typing import Iterable
from binaryast.antlr_parser.JavaParserVisitor import JavaParserVisitor
from binaryast.antlr_parser.JavaParser import JavaParser
from io import StringIO
from antlr4.Token import Token
from antlr4.tree.Tree import Tree


class MethodContextExtractorVisitor(JavaParserVisitor):
    """Extracts all the methodcontexts from an ANTLR tree, which correspond to either the given lines"""

    def __init__(self, lines: list[int]):
        self.lines = lines

    def aggregateResult(self, aggregate, nextResult):
        # makes a list of the results
        if nextResult is None:
            return aggregate
        elif isinstance(nextResult, list):
            if aggregate is None:
                return nextResult
            elif isinstance(aggregate, list):
                aggregate.extend(nextResult)
                return aggregate
        elif nextResult is not None:
            if aggregate is None:
                return [nextResult]
            elif isinstance(aggregate, list):
                aggregate.append(nextResult)
                return aggregate
        else:
            return aggregate

    def _visit(self, ctx: Context):
        if any([ctx.start.line <= line <= ctx.stop.line for line in self.lines]):
            self.lines = [
                line
                for line in self.lines
                if not (ctx.start.line <= line <= ctx.stop.line)
            ]
            return ctx

    def visitMethodDeclaration(self, ctx: JavaParser.MethodDeclarationContext):
        return self._visit(ctx)

    def visitGenericMethodDeclaration(
        self, ctx: JavaParser.GenericMethodDeclarationContext
    ):
        return self._visit(ctx)

    def visitGenericConstructorDeclaration(
        self, ctx: JavaParser.GenericConstructorDeclarationContext
    ):
        return self._visit(ctx)

    def visitConstructorDeclaration(
        self, ctx: JavaParser.ConstructorDeclarationContext
    ):
        return self._visit(ctx)

    def visitEnumDeclaration(self, ctx: JavaParser.EnumDeclarationContext):
        return self._visit(ctx)

    def visitInterfaceMethodDeclaration(
        self, ctx: JavaParser.InterfaceMethodDeclarationContext
    ):
        return self._visit(ctx)
