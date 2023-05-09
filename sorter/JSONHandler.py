import json
from numpy import dtype


# Data classes to encode the json into objects
class Patch:
    def __init__(self, path, score, explanation):
        self.path, self.score, self.explanation = path, score, explanation


class TextRange:
    def __init__(self, endLine, endColumn, startColumn, startLine):
        self.endLine, self.endColumn = endLine, endColumn
        self.startColumn, self.startLine = startColumn, startLine


class PatchesArray:
    def __init__(self, patches, textRange):
        self.patches = [
            Patch(patch["path"], patch["score"], patch["explanation"])
            for patch in patches
        ]
        self.textRange = TextRange(
            textRange["endLine"],
            textRange["endColumn"],
            textRange["startColumn"],
            textRange["startLine"],
        )


class JSONEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, dtype):
            return obj.val()
        try:
            return vars(obj)
        except Exception as e:
            return str(obj)


class Issue:
    def __init__(self, name, patchList):
        self.name = name

        if isinstance(patchList, list):
            self.patchesArray = [
                PatchesArray(patchesArray["patches"], patchesArray["textRange"])
                for patchesArray in patchList
            ]
        elif isinstance(patchList, dict):
            self.patchesArray = [
                PatchesArray(patchList["patches"], patchList["textRange"])
            ]
        else:
            raise TypeError("patchList should be list or dict")


class JSONHandler:
    def __init__(self):
        self.issues = []
        self.initialScores = []

    def parseJSON(self, issuesPath):
        """Parses the json file which contains the issues into objects

        Parameters
        ----------
        issuesPath : str
            The path to the json file

        Returns
        -------
        list[str]
            A list containing all the paths to the patches extracted from the json file
        """
        with open(issuesPath, "rt") as file:
            parsedjsondict = json.load(file)
        self.issues = [Issue(key, parsedjsondict[key]) for key in parsedjsondict.keys()]

        patchPaths = []
        for issue in self.issues:
            for patchesArray in issue.patchesArray:
                for patch in patchesArray.patches:
                    patchPaths.append(patch)
                    self.initialScores.append(patch.score)
        return patchPaths

    def extractPatchesArrays(self) -> list[PatchesArray]:
        """Gets the patches arrays (textrange and a list of candidate patches) from the previously read json file

        Returns
        -------
        list[PatchesArray]
            A list of all the patchesArrays in the json file
        """
        patchesArrays = []
        for issue in self.issues:
            patchesArrays.extend(issue.patchesArray)
        return patchesArrays

    def updateJSON(self, issuesPath, scores):
        """Updates the scores for the patches in the json file

        Parameters
        ----------
        issuesPath :
            The path to the json file
        scores : list[float]
            A list of the scores, the scores should be in the same order as
            previously read from the json file (e. g. with parseJSON method)
        """
        i = 0
        for issue in self.issues:
            for patchlocation in issue.patchesArray:
                for patch in patchlocation.patches:
                    patch.score = scores[i]
                    i += 1

        completejson = dict()
        for issue in self.issues:
            completejson[issue.name] = issue.patchesArray
        with open(issuesPath, "w") as file:
            json.dump(completejson, file, indent=2, cls=JSONEncoder)

    def extract_patches(self):
        """Gets the patches from the previously read json file

        Returns
        -------
        list(Patch)
            A list of all the patches
        """
        patches = []
        for issue in self.issues:
            for patchlocation in issue.patchesArray:
                patches.extend(patchlocation.patches)
        return patches
