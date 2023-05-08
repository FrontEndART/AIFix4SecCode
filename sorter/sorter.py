from email.mime import base
from jsonargparse import CLI
import jsonargparse
from JSONHandler import JSONHandler
from filepatcher import FilePatcher
from vectorizer import Vectorizer
from comparer import Comparer
from statuscollector import StatusCollector, Status
import os
import functools
import time


class SorterError(Exception):
    """Exception when anything unexpected happens during sorting"""


class Sorter:
    def __init__(
        self,
        patchesPath: str,
        basePath: str,
        vectorizer: str,
        comparer: str,
        filePatcherMode: str,
        fileLogLevel: int,
        streamLogLevel: int,
    ):
        """Sorts the generated patches.

        Parameters
        ----------
        patchesPath : str
            Path to a text file which contains the paths to the patches
        basePath: str
            The path, which is appended to the beginning of every file
        vectorizer : str
            The algorithm which will create the vectors from the patched file
        comparer : str
            The algorithm which calculates the scores of the vectorized patched files
        filePatcher : str
            The mode of how the filepatcher patches the source, partial or full.
        fileLogLevel : int
            The logging level for the logfile, 10 DEBUG, 20 INFO, 30, WARNING, 40 ERROR, 50 CRITICAL
        streamLogLevel : int
            The logging level for the console, 10 DEBUG, 20 INFO, 30, WARNING, 40 ERROR, 50 CRITICAL
        """
        self.patchesPath = patchesPath
        self.statuscollector = StatusCollector(fileLogLevel, streamLogLevel)
        self.statuscollector.info(locals())

        self.filePatcher = FilePatcher(basepath=basePath, mode=filePatcherMode)
        self.vectorizer = Vectorizer(vectorizer)
        self.comparer = Comparer(comparer)

    def log(status=None, logFunctionName=False, logExeceutionTime=False):
        """A decorator, to log the statuses after executing the operations. If an exception occurs, then that is handled and logged as well.

        Parameters
        ----------
        status : statuscollector.Status, optional
            The status in which the sorter will be after the decorated function returns, by default None
        """

        def createArgsLogMessage(func, *args, **kwargs):
            if len(args) + len(kwargs) > 0:
                arguments = [repr(arg) for arg in args]
                arguments.extend([f"{key}={value!r}" for key, value in kwargs.items()])
                arguments = ", ".join(arguments)
            else:
                arguments = "no arguments"

            return arguments

        def decorate(func):
            @functools.wraps(func)
            def call(self, *args, **kwargs):
                statuscollector = getattr(self, "statuscollector")
                if logFunctionName:
                    statuscollector.debug(
                        f"{func.__name__} called with {createArgsLogMessage(func, *args, **kwargs)}"
                    )
                if status is not None:
                    statuscollector.setStatus(status)
                start = time.perf_counter()
                try:
                    result = func(self, *args, **kwargs)
                    return result
                except Exception as e:
                    if not logFunctionName:
                        statuscollector.error(
                            f"{func.__name__} called with {createArgsLogMessage(func, *args, **kwargs)}"
                        )
                    statuscollector.exception(
                        f"Exception occured in {func.__name__}: {str(e)}"
                    )
                    raise SorterError(e.__class__.__name__) from e
                finally:
                    end = time.perf_counter()
                    if logExeceutionTime:
                        statuscollector.info(
                            f"Execution time: {end - start:0.4f} seconds"
                        )

            return call

        return decorate

    @log(Status.FilesPatched, True)
    def patchOriginal(self, patch):
        self.patchedOriginal = self.filePatcher.patchFile(patch)
        return self.patchedOriginal

    @log(Status.OriginalExtracted)
    def extractOriginal(self, patch):
        self.original = self.filePatcher.extractOriginal(patch)
        return self.original

    @log(Status.Vectorized)
    def vectorize(self):
        self.vectorizedOriginal, self.vectorizedSource = self.vectorizer.vectorize(
            self.original, self.patchedOriginal
        )
        return self.vectorizedOriginal, self.vectorizedSource

    @log(Status.Compared)
    def compare(self):
        return self.comparer.score(self.vectorizedOriginal, self.vectorizedSource)

    @log(Status.Compared)
    def bulkCompare(self, vectorizedOthers):
        return self.comparer.bulkScore(self.vectorizedOriginal, vectorizedOthers)

    def status(self):
        return self.statuscollector.status.name

    @log(logExeceutionTime=True)
    def sort(self):
        scores = dict()
        with open(self.patchesPath, "rt") as file:
            for patchPath in file.readlines():
                patchPath = patchPath.rstrip()
                try:
                    self.extractOriginal(patchPath)
                    self.patchOriginal(patchPath)
                    self.vectorize()
                    score = self.compare()
                    scores[patchPath] = score
                except SorterError as se:
                    self.statuscollector.exception("SorterError occured")
                    scores[patchPath] = 0
                    continue

        return scores

    @log(logExeceutionTime=True)
    def bulkSort(self):
        scores = []
        self.parseJSON()
        for patchesArray in self.patchesArrays:
            vectors = []
            for patch in patchesArray.patches:
                try:
                    # patchPath = os.path.join(os.path.dirname(self.issuesPath), patch.path)
                    self.extractOriginal(patch.path)
                    self.patchOriginal(patch.path)
                    vectors.append(self.vectorize()[1])
                except SorterError as se:
                    self.statuscollector.exception("SorterError occured")
                    # fix
                    # scores.append(self.jsonHandler.initialScores[index])
            scores.extend(self.bulkCompare(vectors))
        return scores


def sort(
    patchesPath: str,
    resultsPath: str,
    vectorizer: str = "word2vec",
    comparer: str = "cossim",
    filePatcherMode: str = "partial",
    fileLogLevel: int = 10,
    streamLogLevel: int = 10,
    bulk: bool = False,
):
    """Script for sorting the generated pathces.

    Parameters
    patchesPath : str
        Path to a text file which contains the paths to the patches
    resultsPath : str
        Path to a text file which will contain the scores
    vectorizer : str
        The algorithm which will create the vectors from the patched file
    comparer : str
        The algorithm which calculates the scores of the vectorized patched files
    filePatcher : str
        The mode of how the filepatcher patches the source, partial or full.
    fileLogLevel : int
        The logging level for the logfile, 10 DEBUG, 20 INFO, 30, WARNING, 40 ERROR, 50 CRITICAL
    streamLogLevel : int
        The logging level for the console, 10 DEBUG, 20 INFO, 30, WARNING, 40 ERROR, 50 CRITICAL
    bulk : bool
        If true, than the vectors and scores are calculated in bulk for all the patches to the same original source code snippet
    """
    sorter = Sorter(
        patchesPath=patchesPath,
        basePath="",
        vectorizer=vectorizer,
        comparer=comparer,
        filePatcherMode=filePatcherMode,
        fileLogLevel=fileLogLevel,
        streamLogLevel=streamLogLevel,
    )
    if bulk:
        scores = sorter.bulkSort()
    else:
        scores = sorter.sort()
    del sorter
    with open(resultsPath, "wt") as file:
        for patchName, score in scores.items():
            file.write(f"{patchName};{score}\n")


def main():
    parser = jsonargparse.capture_parser(CLI, [sort], parser_mode="jsonnet")
    args = parser.parse_args()
    args.pop("config")
    sort(**args)
    # globals()[args.subcommand](args)


if __name__ == "__main__":
    main()
