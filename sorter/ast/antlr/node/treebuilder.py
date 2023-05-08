from antlr_parser.JavaParserListener import JavaParserListener
from antlr_parser.JavaParser import JavaParser
from node.node import Node


class TreeBuilder(JavaParserListener):
    def __init__(self):
        self.root = None
        self.currentNode = self.root

    def enterCompilationUnit(self, ctx: JavaParser.CompilationUnitContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitCompilationUnit(self, ctx: JavaParser.CompilationUnitContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterPackageDeclaration(self, ctx: JavaParser.PackageDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitPackageDeclaration(self, ctx: JavaParser.PackageDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterImportDeclaration(self, ctx: JavaParser.ImportDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitImportDeclaration(self, ctx: JavaParser.ImportDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterClassDeclaration(self, ctx: JavaParser.ClassDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitClassDeclaration(self, ctx: JavaParser.ClassDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterTypeParameter(self, ctx: JavaParser.TypeParameterContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitTypeParameter(self, ctx: JavaParser.TypeParameterContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterTypeBound(self, ctx: JavaParser.TypeBoundContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitTypeBound(self, ctx: JavaParser.TypeBoundContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterEnumDeclaration(self, ctx: JavaParser.EnumDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitEnumDeclaration(self, ctx: JavaParser.EnumDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterEnumConstants(self, ctx: JavaParser.EnumConstantsContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitEnumConstants(self, ctx: JavaParser.EnumConstantsContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterEnumConstant(self, ctx: JavaParser.EnumConstantContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitEnumConstant(self, ctx: JavaParser.EnumConstantContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterInterfaceDeclaration(self, ctx: JavaParser.InterfaceDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitInterfaceDeclaration(self, ctx: JavaParser.InterfaceDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterMethodDeclaration(self, ctx: JavaParser.MethodDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitMethodDeclaration(self, ctx: JavaParser.MethodDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterGenericMethodDeclaration(
        self, ctx: JavaParser.GenericMethodDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitGenericMethodDeclaration(
        self, ctx: JavaParser.GenericMethodDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterGenericConstructorDeclaration(
        self, ctx: JavaParser.GenericConstructorDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitGenericConstructorDeclaration(
        self, ctx: JavaParser.GenericConstructorDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterConstructorDeclaration(
        self, ctx: JavaParser.ConstructorDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitConstructorDeclaration(self, ctx: JavaParser.ConstructorDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterFieldDeclaration(self, ctx: JavaParser.FieldDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitFieldDeclaration(self, ctx: JavaParser.FieldDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterConstDeclaration(self, ctx: JavaParser.ConstDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitConstDeclaration(self, ctx: JavaParser.ConstDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterConstantDeclarator(self, ctx: JavaParser.ConstantDeclaratorContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitConstantDeclarator(self, ctx: JavaParser.ConstantDeclaratorContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterInterfaceMethodDeclaration(
        self, ctx: JavaParser.InterfaceMethodDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitInterfaceMethodDeclaration(
        self, ctx: JavaParser.InterfaceMethodDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterGenericInterfaceMethodDeclaration(
        self, ctx: JavaParser.GenericInterfaceMethodDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitGenericInterfaceMethodDeclaration(
        self, ctx: JavaParser.GenericInterfaceMethodDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterVariableDeclarator(self, ctx: JavaParser.VariableDeclaratorContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitVariableDeclarator(self, ctx: JavaParser.VariableDeclaratorContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterVariableInitializer(self, ctx: JavaParser.VariableInitializerContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitVariableInitializer(self, ctx: JavaParser.VariableInitializerContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterArrayInitializer(self, ctx: JavaParser.ArrayInitializerContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitArrayInitializer(self, ctx: JavaParser.ArrayInitializerContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterTypeArgument(self, ctx: JavaParser.TypeArgumentContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitTypeArgument(self, ctx: JavaParser.TypeArgumentContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterQualifiedNameList(self, ctx: JavaParser.QualifiedNameListContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitQualifiedNameList(self, ctx: JavaParser.QualifiedNameListContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterReceiverParameter(self, ctx: JavaParser.ReceiverParameterContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitReceiverParameter(self, ctx: JavaParser.ReceiverParameterContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterFormalParameter(self, ctx: JavaParser.FormalParameterContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitFormalParameter(self, ctx: JavaParser.FormalParameterContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLastFormalParameter(self, ctx: JavaParser.LastFormalParameterContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLastFormalParameter(self, ctx: JavaParser.LastFormalParameterContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLambdaLVTIParameter(self, ctx: JavaParser.LambdaLVTIParameterContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLambdaLVTIParameter(self, ctx: JavaParser.LambdaLVTIParameterContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterQualifiedName(self, ctx: JavaParser.QualifiedNameContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitQualifiedName(self, ctx: JavaParser.QualifiedNameContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLiteral(self, ctx: JavaParser.LiteralContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLiteral(self, ctx: JavaParser.LiteralContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterIntegerLiteral(self, ctx: JavaParser.IntegerLiteralContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitIntegerLiteral(self, ctx: JavaParser.IntegerLiteralContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterFloatLiteral(self, ctx: JavaParser.FloatLiteralContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitFloatLiteral(self, ctx: JavaParser.FloatLiteralContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAltAnnotationQualifiedName(
        self, ctx: JavaParser.AltAnnotationQualifiedNameContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAltAnnotationQualifiedName(
        self, ctx: JavaParser.AltAnnotationQualifiedNameContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotation(self, ctx: JavaParser.AnnotationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotation(self, ctx: JavaParser.AnnotationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterElementValuePairs(self, ctx: JavaParser.ElementValuePairsContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitElementValuePairs(self, ctx: JavaParser.ElementValuePairsContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterElementValuePair(self, ctx: JavaParser.ElementValuePairContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitElementValuePair(self, ctx: JavaParser.ElementValuePairContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterElementValue(self, ctx: JavaParser.ElementValueContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitElementValue(self, ctx: JavaParser.ElementValueContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterElementValueArrayInitializer(
        self, ctx: JavaParser.ElementValueArrayInitializerContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitElementValueArrayInitializer(
        self, ctx: JavaParser.ElementValueArrayInitializerContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationTypeDeclaration(
        self, ctx: JavaParser.AnnotationTypeDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationTypeDeclaration(
        self, ctx: JavaParser.AnnotationTypeDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationTypeElementDeclaration(
        self, ctx: JavaParser.AnnotationTypeElementDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationTypeElementDeclaration(
        self, ctx: JavaParser.AnnotationTypeElementDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationTypeElementRest(
        self, ctx: JavaParser.AnnotationTypeElementRestContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationTypeElementRest(
        self, ctx: JavaParser.AnnotationTypeElementRestContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationMethodOrConstantRest(
        self, ctx: JavaParser.AnnotationMethodOrConstantRestContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationMethodOrConstantRest(
        self, ctx: JavaParser.AnnotationMethodOrConstantRestContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationMethodRest(self, ctx: JavaParser.AnnotationMethodRestContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationMethodRest(self, ctx: JavaParser.AnnotationMethodRestContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterAnnotationConstantRest(
        self, ctx: JavaParser.AnnotationConstantRestContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitAnnotationConstantRest(self, ctx: JavaParser.AnnotationConstantRestContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterDefaultValue(self, ctx: JavaParser.DefaultValueContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitDefaultValue(self, ctx: JavaParser.DefaultValueContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterModuleDeclaration(self, ctx: JavaParser.ModuleDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitModuleDeclaration(self, ctx: JavaParser.ModuleDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterModuleDirective(self, ctx: JavaParser.ModuleDirectiveContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitModuleDirective(self, ctx: JavaParser.ModuleDirectiveContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterRequiresModifier(self, ctx: JavaParser.RequiresModifierContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitRequiresModifier(self, ctx: JavaParser.RequiresModifierContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterRecordDeclaration(self, ctx: JavaParser.RecordDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitRecordDeclaration(self, ctx: JavaParser.RecordDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterRecordHeader(self, ctx: JavaParser.RecordHeaderContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitRecordHeader(self, ctx: JavaParser.RecordHeaderContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterRecordComponentList(self, ctx: JavaParser.RecordComponentListContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitRecordComponentList(self, ctx: JavaParser.RecordComponentListContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterRecordComponent(self, ctx: JavaParser.RecordComponentContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitRecordComponent(self, ctx: JavaParser.RecordComponentContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLocalVariableDeclaration(
        self, ctx: JavaParser.LocalVariableDeclarationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLocalVariableDeclaration(
        self, ctx: JavaParser.LocalVariableDeclarationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLocalTypeDeclaration(self, ctx: JavaParser.LocalTypeDeclarationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLocalTypeDeclaration(self, ctx: JavaParser.LocalTypeDeclarationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterStatement(self, ctx: JavaParser.StatementContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitStatement(self, ctx: JavaParser.StatementContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterCatchClause(self, ctx: JavaParser.CatchClauseContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitCatchClause(self, ctx: JavaParser.CatchClauseContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterCatchType(self, ctx: JavaParser.CatchTypeContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitCatchType(self, ctx: JavaParser.CatchTypeContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterFinallyBlock(self, ctx: JavaParser.FinallyBlockContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitFinallyBlock(self, ctx: JavaParser.FinallyBlockContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterResourceSpecification(self, ctx: JavaParser.ResourceSpecificationContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitResourceSpecification(self, ctx: JavaParser.ResourceSpecificationContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterResources(self, ctx: JavaParser.ResourcesContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitResources(self, ctx: JavaParser.ResourcesContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterResource(self, ctx: JavaParser.ResourceContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitResource(self, ctx: JavaParser.ResourceContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSwitchBlockStatementGroup(
        self, ctx: JavaParser.SwitchBlockStatementGroupContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSwitchBlockStatementGroup(
        self, ctx: JavaParser.SwitchBlockStatementGroupContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSwitchLabel(self, ctx: JavaParser.SwitchLabelContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSwitchLabel(self, ctx: JavaParser.SwitchLabelContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterForControl(self, ctx: JavaParser.ForControlContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitForControl(self, ctx: JavaParser.ForControlContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterForInit(self, ctx: JavaParser.ForInitContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitForInit(self, ctx: JavaParser.ForInitContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterEnhancedForControl(self, ctx: JavaParser.EnhancedForControlContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitEnhancedForControl(self, ctx: JavaParser.EnhancedForControlContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterParExpression(self, ctx: JavaParser.ParExpressionContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitParExpression(self, ctx: JavaParser.ParExpressionContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterMethodCall(self, ctx: JavaParser.MethodCallContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitMethodCall(self, ctx: JavaParser.MethodCallContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterPattern(self, ctx: JavaParser.PatternContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitPattern(self, ctx: JavaParser.PatternContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterLambdaExpression(self, ctx: JavaParser.LambdaExpressionContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitLambdaExpression(self, ctx: JavaParser.LambdaExpressionContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterPrimary(self, ctx: JavaParser.PrimaryContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitPrimary(self, ctx: JavaParser.PrimaryContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSwitchExpression(self, ctx: JavaParser.SwitchExpressionContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSwitchExpression(self, ctx: JavaParser.SwitchExpressionContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSwitchLabeledRule(self, ctx: JavaParser.SwitchLabeledRuleContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSwitchLabeledRule(self, ctx: JavaParser.SwitchLabeledRuleContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterGuardedPattern(self, ctx: JavaParser.GuardedPatternContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitGuardedPattern(self, ctx: JavaParser.GuardedPatternContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSwitchRuleOutcome(self, ctx: JavaParser.SwitchRuleOutcomeContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSwitchRuleOutcome(self, ctx: JavaParser.SwitchRuleOutcomeContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterClassType(self, ctx: JavaParser.ClassTypeContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitClassType(self, ctx: JavaParser.ClassTypeContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterCreator(self, ctx: JavaParser.CreatorContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitCreator(self, ctx: JavaParser.CreatorContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterCreatedName(self, ctx: JavaParser.CreatedNameContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitCreatedName(self, ctx: JavaParser.CreatedNameContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterInnerCreator(self, ctx: JavaParser.InnerCreatorContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitInnerCreator(self, ctx: JavaParser.InnerCreatorContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterArrayCreatorRest(self, ctx: JavaParser.ArrayCreatorRestContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitArrayCreatorRest(self, ctx: JavaParser.ArrayCreatorRestContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterClassCreatorRest(self, ctx: JavaParser.ClassCreatorRestContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitClassCreatorRest(self, ctx: JavaParser.ClassCreatorRestContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterExplicitGenericInvocation(
        self, ctx: JavaParser.ExplicitGenericInvocationContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitExplicitGenericInvocation(
        self, ctx: JavaParser.ExplicitGenericInvocationContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterTypeArgumentsOrDiamond(
        self, ctx: JavaParser.TypeArgumentsOrDiamondContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitTypeArgumentsOrDiamond(self, ctx: JavaParser.TypeArgumentsOrDiamondContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterNonWildcardTypeArgumentsOrDiamond(
        self, ctx: JavaParser.NonWildcardTypeArgumentsOrDiamondContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitNonWildcardTypeArgumentsOrDiamond(
        self, ctx: JavaParser.NonWildcardTypeArgumentsOrDiamondContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterNonWildcardTypeArguments(
        self, ctx: JavaParser.NonWildcardTypeArgumentsContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitNonWildcardTypeArguments(
        self, ctx: JavaParser.NonWildcardTypeArgumentsContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterTypeArguments(self, ctx: JavaParser.TypeArgumentsContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitTypeArguments(self, ctx: JavaParser.TypeArgumentsContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterSuperSuffix(self, ctx: JavaParser.SuperSuffixContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitSuperSuffix(self, ctx: JavaParser.SuperSuffixContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterExplicitGenericInvocationSuffix(
        self, ctx: JavaParser.ExplicitGenericInvocationSuffixContext
    ):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitExplicitGenericInvocationSuffix(
        self, ctx: JavaParser.ExplicitGenericInvocationSuffixContext
    ):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent

    def enterArguments(self, ctx: JavaParser.ArgumentsContext):
        if self.root == None:
            node = Node(ctx)
            self.root = node
            self.currentNode = node
        else:
            node = Node(ctx, self.currentNode)
            self.currentNode.addChild(node)
            self.currentNode = node

    def exitArguments(self, ctx: JavaParser.ArgumentsContext):
        if self.currentNode == None:
            self.currentNode = self.root
        else:
            self.currentNode = self.currentNode.parent
