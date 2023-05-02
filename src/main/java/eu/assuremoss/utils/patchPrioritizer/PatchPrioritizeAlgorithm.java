package eu.assuremoss.utils.patchPrioritizer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public interface PatchPrioritizeAlgorithm {
    List<Pair<Pair<File, Pair<Patch<String>, String>>, Double>> prioritize(List<Pair<File, Pair<Patch<String>, String>>> patches);
}
