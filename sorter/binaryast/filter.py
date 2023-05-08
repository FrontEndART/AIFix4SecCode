from random import sample
import vectorizer
import node.node as node
import numpy as np
from sklearn.decomposition import PCA
from sklearn.preprocessing import StandardScaler
import pickle


def extractMethodsFromSourceFiles(pathFile: str) -> list[np.ndarray]:
    methods = list()
    vt = vectorizer.BinaryASTVectorizer()
    count = 0
    with open(pathFile, "rt") as file:
        for line in file:
            print(count)
            with open(line.rstrip(), "rt") as sourcefile:
                try:
                    source = sourcefile.read()
                except UnicodeDecodeError as ude:
                    continue
            tree = vt.createAST(source)
            methods.extend(extractMethods(tree))
            count += 1
    return [node.binarize(method).vector(15) for method in methods]


def extractMethods(tree: node.Node) -> list[node.Node]:
    methods = list()
    for child in tree.children:
        if child.name == "MethodDeclarationContext":
            methods.append(child)
        else:
            methods.extend(extractMethods(child))
    return methods


def fitAndSavePCA(vectors):
    pca = PCA(0.95)
    # scaler = StandardScaler()
    # vectors = scaler.fit_transform(vectors)
    pca.fit(vectors)
    with open("pca", "wb") as file:
        pickle.dump(pca, file)


def selectRandomPaths(pathsPath, savePath, count):
    with open(pathsPath, "rt", encoding="utf-8") as file:
        lines = file.readlines()
    randompaths = sample(lines, count)
    with open(savePath, "wt") as paths:
        paths.writelines(randompaths)


if "__name__" == "__main__":
    # selectRandomPaths("allpaths.txt", "paths.txt", 500)
    fitAndSavePCA(extractMethodsFromSourceFiles("paths.txt"))
