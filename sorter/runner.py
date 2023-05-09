import argparse
from typing import Iterable, Tuple
import sorter_from_json as st
import JSONHandler
import comparer
import vectorizer

import shutil
import csv
import os
import itertools

# Preffered combinations, where it makes sense to pair the choosen comparer with the vectorizer
COMBINATIONS = [
    ("character", "hamming"),
    # ("character", "levenshtein"),
    ("token", "hamming"),
    # ("token", "levenshtein"),
    ("word2vec", "cossim"),
    ("word2vec", "euclidean"),
    ("word2vec", "standardizedeuclidean"),
    ("word2vec", "manhattan"),
    ("word2vec", "minkowski"),
    ("word2vec", "relativizedeuclidean"),
    ("word2vec", "canberra"),
    ("word2vec", "trianglesimilarity"),
    ("word2vec", "sectorsimilarity"),
    ("word2vec", "tsss"),
    ("doc2vec", "cossim"),
    ("doc2vec", "euclidean"),
    ("doc2vec", "standardizedeuclidean"),
    ("doc2vec", "manhattan"),
    ("doc2vec", "minkowski"),
    ("doc2vec", "relativizedeuclidean"),
    ("doc2vec", "canberra"),
    ("doc2vec", "trianglesimilarity"),
    ("doc2vec", "sectorsimilarity"),
    ("doc2vec", "tsss"),
    ("glove", "cossim"),
    ("glove", "euclidean"),
    ("glove", "standardizedeuclidean"),
    ("glove", "manhattan"),
    ("glove", "minkowski"),
    ("glove", "relativizedeuclidean"),
    ("glove", "canberra"),
    ("glove", "trianglesimilarity"),
    ("glove", "sectorsimilarity"),
    ("glove", "tsss"),
    ("bagofwords", "jaccard"),
    ("bagofwords", "sorensendice"),
    ("bagofwords", "cossim"),
    ("bagofwords", "euclidean"),
    ("bagofwords", "standardizedeuclidean"),
    ("bagofwords", "manhattan"),
    ("bagofwords", "minkowski"),
    ("bagofwords", "relativizedeuclidean"),
    ("bagofwords", "canberra"),
    ("bagofwords", "trianglesimilarity"),
    ("bagofwords", "sectorsimilarity"),
    ("bagofwords", "tsss"),
    ("bagofwords", "hamming"),
    ("binaryast", "cossim"),
    ("binaryast", "euclidean"),
    ("binaryast", "standardizedeuclidean"),
    ("binaryast", "manhattan"),
    ("binaryast", "minkowski"),
    ("binaryast", "relativizedeuclidean"),
    ("binaryast", "canberra"),
    ("binaryast", "trianglesimilarity"),
    ("binaryast", "sectorsimilarity"),
    ("binaryast", "tsss"),
    ("bagofwords", "hamming"),
    # ("bagofwords", "levenshtein"),
]


def run_configuration(
    original_patch_file: str,
    vectorizer_alg: str,
    comparer_alg: str,
    filepatcher_mode: str,
    bulk: bool = False,
):
    """Runs a vectorizer_comparer_filepatcher_bulk configuration on the sorter.

    Parameters
    ----------
    original_patch_file : str
        Path to the issues file (the json file)
    vectorizer_alg : str
        Name of the vectorizer algorithm
    comparer_alg : str
        Name of the comparer algorithm
    filepatcher_mode : str
        Name of the filepatcher mode
    bulk : bool, optional
        True if the bulk score calculating method is used, by default False

    Returns
    -------
    list[Patch]
        The patches with the updated scores
    """

    patchFilePath = os.path.join(
        os.path.dirname(original_patch_file),
        "".join(["issues_", vectorizer_alg, "_", comparer_alg, ".json"]),
    )
    shutil.copy(original_patch_file, patchFilePath)
    st.sort(
        issuesPath=patchFilePath,
        vectorizer=vectorizer_alg,
        comparer=comparer_alg,
        filePatcherMode=filepatcher_mode,
        fileLogLevel=30,
        streamLogLevel=10,
        bulk=bulk,
    )

    jsonhandler = JSONHandler.JSONHandler()
    jsonhandler.parseJSON(patchFilePath)
    patches = jsonhandler.extract_patches()
    os.remove(patchFilePath)
    return patches


def create_parser():
    """Creates an argument parser with issuesPath and combinations arguments.

    Returns
    -------
    argparse.ArgumentParser
        The argument parser with the two parameters
    """
    parser = argparse.ArgumentParser(
        description="Script for running the sorter with all the vectorizer-comparer pairs."
    )
    parser.add_argument(
        "--issuesPath",
        help="The json file, which contains information about the generated pathces",
    )
    parser.add_argument(
        "--combinations",
        help="Run all or preferred combinations of vectors and comparers (value 'all'/'preferred')",
        default="preferred",
        choices=["all", "preferred"],
    )
    return parser


def run_combinations(issuesPath: str, vectorizers_comparers: Iterable[Tuple[str, str]]):
    """Runs the sorter with the given vectorizer comparer combinations with all the filepatcher and bulk combinations.
        Saves the result (the scores) into a .csv file.

    Parameters
    ----------
    issuesPath : str
        Path to the issues (.json) file
    vectorizers_comparers : Iterable[Tuple[str, str]]
        Vectorizer-comparer combinations
    """
    jsonhandler = JSONHandler.JSONHandler()
    patches = jsonhandler.parseJSON(issuesPath)

    # headers in the .csv file
    fieldnames = [
        "vectorizer_alg",
        "comparer_alg",
        "filepatcher_mode",
        "bulk",
    ]
    for patch in patches:
        fieldnames.append(patch.path)

    with open("results.csv", "w", newline="") as csv_file:
        writer = csv.DictWriter(csv_file, delimiter=";", fieldnames=fieldnames)
        writer.writeheader()
        # go through all combinations
        for vectorizer_comparer in vectorizers_comparers:
            for config in itertools.product(
                ("partial", "full", "method"), (True, False)
            ):
                row = dict(zip(fieldnames, [*vectorizer_comparer, *config]))
                patches = run_configuration(issuesPath, *vectorizer_comparer, *config)
                for patch in patches:
                    row[patch.path] = patch.score
                writer.writerow(row)


def run_all(issuesPath: str):
    """Runs the sorter with all possible configurations.
        Saves the result (the scores) into a .csv file.

    Parameters
    ----------
    issuesPath : str
        Path to issues (.json) file
    """
    vectorizers = [
        subclass.__name__.lower()
        for subclass in vectorizer.VectorizerStrategy.__subclasses__()
    ]
    comparers = [
        subclass.__name__.lower()
        for subclass in comparer.ComparerStrategy.__subclasses__()
    ]
    run_combinations(issuesPath, itertools.product(vectorizers, comparers))


def main():
    parser = create_parser()
    args = parser.parse_args()
    if args.combinations == "all":
        run_all(args.issuesPath)
    elif args.combinations == "preferred":
        run_combinations(args.issuesPath, COMBINATIONS)


if __name__ == "__main__":
    main()
