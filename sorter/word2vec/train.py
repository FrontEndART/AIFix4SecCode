import argparse
from gensim.models.word2vec import Word2Vec
import gensim.models.keyedvectors as keyedvectors
import logging
from os.path import isfile
from os import cpu_count
from smart_open import open as sopen

from callbacks import SaveCallback


def train_word2vec(path, modelPath, epochs=1, vector_size=100):
    corpus_iterable = LineSentenceFromTarGz(path)
    callbacks = [SaveCallback(modelPath)]
    workers = cpu_count() - 1

    model = Word2Vec(
        sentences=corpus_iterable,
        corpus_file=None,
        vector_size=vector_size,
        alpha=0.025,
        window=5,
        min_count=5,
        max_vocab_size=None,
        sample=1e-3,
        seed=1,
        workers=workers,
        min_alpha=0.0001,
        sg=0,
        hs=0,
        negative=5,
        ns_exponent=0.75,
        cbow_mean=1,
        hashfxn=hash,
        epochs=epochs,
        null_word=0,
        trim_rule=None,
        sorted_vocab=1,
        compute_loss=False,
        callbacks=callbacks,
        comment=None,
        max_final_vocab=None,
        shrink_windows=True,
    )
    print(model.wv.key_to_index)
    model.save(modelPath)


class LineSentenceFromTarGz:
    """Similar to gensim.models.word2vec.LineSentence.
    Iterable, iterates through all the lines in a tar.gz file, which contains the sentences to train the word2vec model.
    """

    def __init__(self, source, max_sentence_length=10000):
        self.source = source
        self.max_sentence_length = max_sentence_length

    def __iter__(self):
        with sopen(self.source, "rb") as fin:
            for line in fin:
                try:
                    line = str(line, encoding="utf-8", errors="strict")
                    # The tar file has headers for each of the files,
                    # the quickest way to ignore that is to find the part of the lines where the first keyword is.
                    line = line[line.find("Keyword") :]
                    # Strips the padding zero bytes from the end
                    line = line.rstrip("\x00 \r\n").split(" ")
                    i = 0
                    while i < len(line):
                        yield line[i : i + self.max_sentence_length]
                        i += self.max_sentence_length
                except Exception as e:
                    logging.debug(e)


def _create_parser():
    parser = argparse.ArgumentParser("Script to train word2vec model.")
    parser.add_argument(
        "-path",
        type=str,
        help="The path to the tar.gz/bz2 file which contains the tokenized source code files.",
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
    if not isfile(args.path):
        raise ValueError("the path to the training data is not a file")


def main():
    logging.basicConfig(
        filename="train.log",
        level=logging.DEBUG,
        format="%(asctime)s:%(levelname)s:%(name)s:%(message)s",
    )
    parser = _create_parser()
    args = parser.parse_args()
    _check_arguments(args)
    train_word2vec(args.path, args.modelPath, epochs=10, vector_size=10)
    # train_word2vec(args.path, args.modelPath, epochs=10, vector_size=50)
    # train_word2vec(args.path, args.modelPath, epochs=10, vector_size=100)
    # train_word2vec(args.path, args.modelPath, epochs=10, vector_size=200)


if __name__ == "__main__":
    main()
