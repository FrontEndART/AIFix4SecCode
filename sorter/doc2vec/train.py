import argparse
import gensim.models.doc2vec as doc2vec
import logging
import os.path
from os import cpu_count
from smart_open import open as sopen

from callbacks import SaveCallback


def train_doc2vec(path, modelPath, epochs=1, vector_size=100):
    corpus_iterable = TaggedDocumentsFromTarGz(path)
    callbacks = [SaveCallback(modelPath)]
    workers = cpu_count() - 1

    model = doc2vec.Doc2Vec(
        documents=corpus_iterable,
        corpus_file=None,
        vector_size=vector_size,
        dm_mean=None,
        dm=1,
        dbow_words=0,
        dm_concat=0,
        dm_tag_count=1,
        dv=None,
        dv_mapfile=None,
        comment=None,
        trim_rule=None,
        callbacks=callbacks,
        window=5,
        workers=workers,
        epochs=epochs,
        shrink_windows=True,
    )
    model.save(modelPath)


class TaggedDocumentsFromTarGz:
    """Similar to gensim.models.doc2vec.TaggedLineDocument
    Iterable, iterates through all the lines in a tar.gz file, which contains the documents to train the doc2vec model.
    """

    def __init__(self, source):
        self.source = source

    def __iter__(self):
        with sopen(self.source, "rb") as fin:
            for item_no, line in enumerate(fin):
                try:
                    line = str(line, encoding="utf-8", errors="strict")
                    # The tar file has headers for each of the files,
                    # the quickest way to ignore that is to find the part of the lines where the first keyword is.
                    line = line[line.find("Keyword") :]
                    # Strips the padding zero bytes from the end
                    line = line.rstrip("\x00 \r\n").split(" ")
                    yield doc2vec.TaggedDocument(line, [item_no])
                except Exception as e:
                    logging.debug(e)


def _create_parser():
    parser = argparse.ArgumentParser("Script to train doc2vec model.")
    parser.add_argument(
        "-path",
        type=str,
        help="The path to the tar.gz/tar.bz2 file which contains the tokenized source code files.",
        default=None,
    )
    parser.add_argument(
        "-modelPath",
        type=str,
        help="Path to a file, where the model will be saved.",
        default=None,
    )
    return parser


def _check_arguments(args):
    if args.path == None:
        raise ValueError("the path argument should be given")
    if not os.path.isfile(args.path):
        raise ValueError("the path to the training data is not a file")


def main():
    logging.basicConfig(
        filename="train.log",
        level=logging.DEBUG,
        format="%(asctime)s:%(levelname)s:%(name)s:%(message)s",
    )
    parser = _create_parser()
    args = parser.parse_args()
    try:
        _check_arguments(args)
    except ValueError as ve:
        print(ve)
        return 1
    train_doc2vec(args.path, args.modelPath, epochs=10, vector_size=100)


if __name__ == "__main__":
    main()
