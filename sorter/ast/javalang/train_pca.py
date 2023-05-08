from sklearn.preprocessing import StandardScaler
import ast_javalang
import binarize
from sklearn.decomposition import PCA


def fitScaler():
    scaler = StandardScaler()
    vectors = []
    with open("pca_train.txt", "rt") as file:
        for line in file:
            try:
                tree = ast_javalang.tree_from_token_strings(line)
                node = binarize.makeTree(tree)
                binarytree = binarize.binarize(node)
                vector = binarize.vectorize(binarytree, 10)
                vectors.append(vector)
            except Exception as e:
                pass
    scaler.fit(vectors)
    return scaler


def fitPca(scaler):
    vectors = []
    with open("pca_train.txt", "rt") as file:
        for line in file:
            try:
                tree = ast_javalang.tree_from_token_strings(line)
                node = binarize.makeTree(tree)
                binarytree = binarize.binarize(node)
                vector = binarize.vectorize(binarytree, 10)
                vectors.append(vector)
            except Exception as e:
                pass
    vectors = scaler.transform(vectors)
    pca = PCA(0.95)
    pca.fit(vectors)
    return pca


tokens = """Keyword_package Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_; Keyword_import Identifier Separator_. Identifier Separator_. Identifier Separator_; Keyword_import Identifier Separator_. Identifier Separator_. Identifier Separator_; Keyword_import Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_. Identifier Separator_; Modifier_public Keyword_class Identifier Keyword_extends Identifier Separator_{ Modifier_private Modifier_final Identifier Identifier Separator_; Modifier_public Identifier Separator_( Identifier Identifier Separator_) Separator_{ Keyword_super Separator_( Identifier Separator_) Separator_; Identifier Operator_= Identifier Separator_. Identifier Operator_? Identifier Separator_. Identifier Separator_( Keyword_this Separator_) Operator_: Null Separator_; Separator_} Annotation Identifier Modifier_public Keyword_void Identifier Separator_( BasicType_int Identifier Separator_) Separator_{ Keyword_if Separator_( Identifier Operator_!= Null Separator_) Separator_{ Keyword_if Separator_( Identifier Operator_== Identifier Separator_) Separator_{ Identifier Separator_( Separator_) Separator_; Separator_} Keyword_else Keyword_if Separator_( Identifier Operator_== Identifier Separator_) Separator_{ Identifier Separator_( Identifier Separator_) Separator_; Separator_} Separator_} Keyword_super Separator_. Identifier Separator_( Identifier Separator_) Separator_; Separator_} Modifier_public BasicType_float Identifier Separator_( Separator_) Separator_{ Keyword_if Separator_( Identifier Separator_. Identifier Separator_) Separator_{ Keyword_return Identifier Separator_. Identifier Separator_( Separator_) Separator_; Separator_} Keyword_else Separator_{ Keyword_return Keyword_super Separator_. Identifier Separator_( Separator_) Separator_; Separator_} Separator_} Modifier_public Keyword_void Identifier Separator_( BasicType_float Identifier Separator_) Separator_{ Keyword_if Separator_( Identifier Separator_. Identifier Separator_) Separator_{ Identifier Separator_. Identifier Separator_( Identifier Separator_) Separator_; Separator_} Keyword_else Separator_{ Keyword_super Separator_. Identifier Separator_( Identifier Separator_) Separator_; Separator_} Separator_} Separator_}"""


def main():
    scaler = fitScaler()
    pca = fitPca(scaler)

    tree = ast_javalang.tree_from_token_strings(tokens)
    print(tree)
    node = binarize.makeTree(tree)
    binarytree = binarize.binarize(node)
    binarize.printBinTree(binarytree)
    vector = binarize.vectorize(binarytree, 10)
    print(vector.tolist())
    vector = pca.transform([vector])
    print(vector)
    print(vector.shape)


if __name__ == "__main__":
    main()
