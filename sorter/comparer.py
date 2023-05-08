from typing import Iterable, Tuple
import numpy as np
from sklearn.preprocessing import StandardScaler
from scipy.spatial.distance import minkowski


class Comparer:
    def __init__(self, comparer_alg: str):
        """Chooses the comparer algorithm based on its name

        Parameters
        ----------
        comparer_alg : str
            Name of the comparer algorithm
        """
        # makes the dictionary of the existing implementations
        # the implementations name can be used as a lowercase name
        # with or without ending in 'comparer'
        implementations = dict()
        for subclass in ComparerStrategy.__subclasses__():
            implementations[subclass.__name__.lower()[: -len("comparer")]] = subclass
            implementations[subclass.__name__.lower()] = subclass

        # if the implementation doesn't exist the cossim will be used as default
        if comparer_alg.lower() in implementations:
            self.comparer = implementations[comparer_alg.lower()]()
        else:
            self.comparer = CosSimComparer()

    def score(self, original, other) -> float:
        """Calculates the similarity score of the two vectors with the given comparer strategy.

        Parameters
        ----------
        original : list[float]/numpy.ndarray
            The vector representing the original source code
        other : list[float]/numpy.ndarray
            The vector representing the patched source code

        Returns
        -------
        float
            The similarity score of the vectors.
        """
        # convert into a numpy array
        if type(original) != np.ndarray:
            original = np.array(original)
        if type(other) != np.ndarray:
            other = np.array(other)
        return self.comparer.score(original, other)

    def bulkScore(self, original, others) -> list[float]:
        """Calculates the similarity score of all the other vectors with the original vector with the given comparer strategy.

        Parameters
        ----------
        original : list[float]/numpy.ndarray
            The vector representing the original source code
        others : list[list[float]]/list[numpy.ndarray]
            The vectors representing the patched source codes

        Returns
        -------
        list[float]
            The similarity score for each of the other vectors

        Raises
        ------
        TypeError
            If the other vectors are a list of numpy arrays or a list of lists
        """
        # convert into a numpy array
        if type(original) != np.ndarray:
            original = np.array(original)
        if type(others) != np.ndarray:
            if hasattr(others, "__iter__"):
                # if its a list of lists, convert into list of numpy arrays
                if all(
                    [
                        type(element) == list or type(element) == np.ndarray
                        for element in others
                    ]
                ):
                    others = [np.array(elem) for elem in others]
                else:
                    raise TypeError()
            else:
                raise TypeError()
        return self.comparer.bulkScore(original, others)


class ComparerStrategy:
    def _resize(
        self, original: np.ndarray, other: np.ndarray
    ) -> Tuple[np.ndarray, np.ndarray]:
        """Resizes the vectors to the of the bigger vector, the smaller vector is padded with zeros.
        Parameters
        ----------
        original : numpy.ndarray
            The vector representing the original source code
        other : numpy.ndarray
            The vector representing the patched source code
        """
        len_original, len_other = len(original), len(other)
        if len_original < len_other:
            original = np.append(original, np.zeros((len_other - len_original)))
        elif len_other < len_original:
            other = np.append(other, np.zeros((len_original - len_other)))

        return original, other

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        """Compares the two given vectors.

        Parameters
        ----------
        original : numpy.ndarray
            The vector representing the original source code
        other : numpy.ndarray
            The vector representing the patched source code

        Returns
        -------
        float
            A score, which represents how similar the two vectors are (10 best, 0 worst)
        """
        raise NotImplementedError()

    def convertToScore(self, value: float) -> float:
        """Converts the similarity/distance value into a score

        Parameters
        ----------
        value : float
            The similarity/distance value to be converted

        Returns
        -------
        float
            A similarity score ranging from 0 to 10.
        """
        return 10 * value

    def bulkCompare(
        self, original: np.ndarray, others: list[np.ndarray]
    ) -> list[float]:
        """Compares the other vectors with the original vector and calculates a similarity value

        Parameters
        ----------
        original : numpy.ndarray
            The vector representing the original source code
        others : list[numpy.ndarray]
            The vectors representing the patched source codes

        Returns
        -------
        list[float]
            The list of similarity/distance values
        """
        return [self.compare(original, other) for other in others]

    def bulkConvertToScore(self, values: list[float]) -> list[float]:
        """Converts the similarity/distance values into a score

        Parameters
        ----------
        values : list[float]
             The similarity/distance values to be converted

        Returns
        -------
        list[float]
            The similarity score for each of the other vectors ranging from 0 to 10
        """
        return [self.convertToScore(value) for value in values]

    def score(self, original: np.ndarray, other: np.ndarray) -> float:
        """Compares the other vectors with the original vector and calculates the similarity score

        Parameters
        ----------
        original : numpy.ndarray
            The vector representing the original source code
        other : numpy.ndarray
            The vector representing the patched source code

        Returns
        -------
        float
            A similarity score ranging from 0 to 10.

        Raises
        ------
        TypeError
            If the given vectors are not numpy arrays
        ValueError
            If the given numpy vectors are not one dimensional
        """
        if type(other) != np.ndarray or type(original) != np.ndarray:
            raise TypeError("The vectors should be numpy arrays")
        if getattr(other, "ndim", 0) != 1 or getattr(original, "ndim", 0) != 1:
            raise ValueError("The dimension should be 1")

        return self.convertToScore(self.compare(original, other))

    def bulkScore(
        self, original: np.ndarray, others: Iterable[np.ndarray]
    ) -> list[float]:
        """Compares the other vectors with the original vector and calculates similarity scores

        Parameters
        ----------
        original : numpy.ndarray
            The vector representing the original source code
        others : list[numpy.ndarray]
            The vectors representing the patched source codes

        Returns
        -------
        list[float]
            The similarity score for each of the other vectors ranging from 0 to 10

        Raises
        ------
        TypeError
            If the original vector is not a numpy array
        ValueError
            If the original vector is not one dimensional
        TypeError
            If the others parameter is not iterable
        TypeError
            If the other vectors are not one dimensional
        """
        if type(original) != np.ndarray:
            raise TypeError("The original vector should be a numpy array")
        if getattr(original, "ndim", 0) != 1:
            raise ValueError("The dimension should be 1")
        if not hasattr(others, "__iter__"):
            raise TypeError("The others parameters should be an iterable")
        if not all([getattr(other, "ndim", 0) != 1] for other in others):
            raise TypeError("The vectors should be numpy arrays and have 1 dimension")
        return self.bulkConvertToScore(self.bulkCompare(original, others))


class CosSimComparer(ComparerStrategy):
    """Compares the vectors using the cosine similarity metric."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        other_length = np.linalg.norm(other)
        original_length = np.linalg.norm(original)
        if np.isclose(other_length * original_length, 0.0):
            return 0
        return np.dot(other, original) / (other_length * original_length)

    def convertToScore(self, value: float) -> float:
        # 180 degree angle is converted to 0; 90 degree angle is converted to 5.0
        # 0 degree angle is converted to 10.0
        return (value + 1) / 2 * 10


def bulkConvertToScoreUsingMaximalDistance(self, values):
    # maximal distance gets 0 score, the others are scaled accordingly
    maximum = np.max(values)
    return 10 - values / maximum * 10


def convertToScoreUsingReciproc(self, value):
    # 1+ is there to guard against division by zero (only if value cannot be negative)
    return 10 / (1 + value)


class EuclideanComparer(ComparerStrategy):
    """Compares the two vectors using the Euclidean norm."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        return np.linalg.norm(other - original)

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class StandardizedEuclideanComparer(ComparerStrategy):
    """Compares the vectors by standardizing them column-wise and calculating the Euclidean norm."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        scaler = StandardScaler()
        scaled = scaler.fit_transform([original, other])
        return np.linalg.norm(scaled[0] - scaled[1])

    convertToScore = convertToScoreUsingReciproc

    def bulkCompare(
        self, original: np.ndarray, others: list[np.ndarray]
    ) -> list[float]:
        # resizes the vectors such that all the vectors have the same dimension,
        # the new dimension is the highest dimension of all the vectors
        newLength = max([len(other) for other in others])
        newLength = len(original) if len(original) > newLength else newLength
        if len(original < newLength):
            original = np.append(original, np.zeros((newLength - len(original))))
        for i in range(len(others)):
            if len(others[i]) < newLength:
                others[i] = np.append(others[i], np.zeros((newLength - len(others[i]))))
        # standardizes all the vectors
        self.scaler = StandardScaler()
        self.scaler.fit(others)
        others_scaled = self.scaler.transform(others)
        original_scaled = self.scaler.transform(original.reshape(1, -1))
        return np.linalg.norm(original_scaled - others_scaled, axis=1)

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class ManhattanComparer(ComparerStrategy):
    """Compares the vectors using the Manhattan distance."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        return np.sum(np.abs(original - other))

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class MinkowskiComparer(ComparerStrategy):
    """Compares the vectors using the Minkowski distance (p = 1.5 by default)."""

    def __init__(self, p=1.5):
        self.p = p

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        return minkowski(original, other, self.p)

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class RelativizedEuclideanComparer(ComparerStrategy):
    """Compares the vectors by normalizing and calculating the Euclidean norm."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        # if one of the vectors is a null vector, then divison cannot be done by its length
        if np.all(original == 0):
            return np.linalg.norm(other / np.linalg.norm(other))
        if np.all(other == 0):
            return np.linalg.norm(original / np.linalg.norm(original))
        return np.linalg.norm(
            original / np.linalg.norm(original) - other / np.linalg.norm(other)
        )

    def convertToScore(self, value: float) -> float:
        return 10 - value / 2 * 10

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class CanberraComparer(ComparerStrategy):
    """Compares the vectors using Canberra distance"""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        # vector dimensionality is saved for the score conversion
        self.vectorLength = len(original)
        return np.sum(np.abs(original - other) / (np.abs(original) + np.abs(other)))

    def convertToScore(self, value: float) -> float:
        # every term of the summation is less then or equal to 1, so the total sum will be
        # less then or equal to the number of dimensions
        return 10 * (1 - value / self.vectorLength)


class HammingComparer(ComparerStrategy):
    """Calculates the Hamming distance between the two vectors."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        return (np.array(original) == np.array(other)).sum() / len(original)


class JaccardComparer(ComparerStrategy):
    """Calculates the Jaccard similarity between the two vectors. Useful for bag of words vectorizer."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        #the vectors are treated as sets
        original, other = self._resize(original, other)
        original_binary = np.where(original > 0, 1, 0)
        other_binary = np.where(other > 0, 1, 0)
        dot = np.dot(original_binary, other_binary)
        return dot / (
            np.dot(original_binary, original_binary)
            + np.dot(other_binary, other_binary)
            - dot
        )


class SorensenDiceComparer(ComparerStrategy):
    """Calculates the Sorensen-Dice index for the two vectors. Useful for bag of words vectorizer."""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        # the vectors are treated as sets
        original, other = self._resize(original, other)
        original_binary = np.where(original > 0, 1, 0)
        other_binary = np.where(other > 0, 1, 0)
        dot = np.dot(original_binary, other_binary)
        return (
            2
            * dot
            / (
                np.dot(original_binary, original_binary)
                + np.dot(other_binary, other_binary)
            )
        )


# class LevenshteinComparer(ComparerStrategy):
#     """Calculates the Levenshtein edit distance between the vectors"""

#     def _levenshtein(self, original, vector):
#         distances = np.zeros((original.shape[0], vector.shape[0]), dtype=np.int32)
#         for i in range(0, original.shape[0]):
#             distances[i, 0] = i

#         for j in range(0, vector.shape[0]):
#             distances[0, j] = j

#         for i, original_value in enumerate(original):
#             for j, vector_value in enumerate(vector):
#                 cost = 0 if original_value == vector_value else 1
#                 distances[i, j] = np.min(
#                     [
#                         distances[i - 1, j] + 1,
#                         distances[i, j - 1] + 1,
#                         distances[i - 1, j - 1] + cost,
#                     ]
#                 )

#         return distances[-1, -1]

#     def compare(self, original: np.ndarray, other: np.ndarray) -> float:
#         print(original.shape, other.shape)
#         if original.shape[0] == 0 or other.shape[0] == 0:
#             return abs(original.shape[0] - other.shape[0])
#         return self._levenshtein(original, other) / np.max(
#             (original.shape[0], other.shape[0])
#         )

#     def convertToScore(self, value: float) -> float:
#         return 10 - value * 10


class TriangleSimilarityComparer(ComparerStrategy):
    """Calculates the Triangle Similarity between the vectors as described in https://doi.org/10.1109/BigDataService.2016.14"""

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        original_length = np.linalg.norm(original)
        other_length = np.linalg.norm(other)
        #an area is calculated, which is the area of the triangle enclosed by the vectors
        #the angle between the vectors is raised by 10 degrees, to account for the case, when the vectors are paralel
        if np.isclose(original_length, 0) or np.isclose(other_length, 0):
            angle = np.radians(90) + np.radians(10)
        else:
            angle = np.arccos(
                round(np.dot(other, original) / (original_length * other_length), 6)
            ) + np.radians(10)
        return original_length * other_length * np.sin(angle) / 2

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class SectorSimilarityComparer(ComparerStrategy):
    """Calculates the Sector Similarity between the vectors as described in https://doi.org/10.1109/BigDataService.2016.14"""

    def _magnitudeDifference(self, original, vector):
        return np.abs(np.linalg.norm(original) - np.linalg.norm(vector))

    def _euclideanDistance(self, original, vector):
        return np.linalg.norm(original - vector)

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        original, other = self._resize(original, other)
        original_length = np.linalg.norm(original)
        other_length = np.linalg.norm(other)
        #the angle between the vectors is raised by 10 degrees, to account for the case, when the vectors are paralel
        if np.isclose(original_length, 0) or np.isclose(other_length, 0):
            angle = np.radians(90) + np.radians(10)
        else:
            angle = np.arccos(
                round(np.dot(other, original) / (original_length * other_length), 6)
            ) + np.radians(10)

        return (
            (
                self._euclideanDistance(original, other)
                + self._magnitudeDifference(original, other)
            )
            ** 2
            * angle
            / 2
            * np.pi
        )

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance


class TSSSComparer(ComparerStrategy):
    """Calculates the TS-SS simlarity between the vectors as described in https://doi.org/10.1109/BigDataService.2016.14"""

    def __init__(self) -> None:
        self.ts = TriangleSimilarityComparer()
        self.ss = SectorSimilarityComparer()

    def compare(self, original: np.ndarray, other: np.ndarray) -> float:
        return self.ts.compare(original, other) * self.ss.compare(original, other)

    convertToScore = convertToScoreUsingReciproc

    bulkConvertToScore = bulkConvertToScoreUsingMaximalDistance
