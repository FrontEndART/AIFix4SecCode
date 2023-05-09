package eu.assuremoss.utils.patchPrioritizer;


import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public class PatchPrioritizer {
    private final PatchPrioritizeAlgorithm patchPrioritizeAlgorithm;

    public PatchPrioritizer(PatchPrioritizeAlgorithm patchPrioritizeAlgorithm) {
        this.patchPrioritizeAlgorithm = patchPrioritizeAlgorithm;
    }

    public List<Pair<Pair<File, Pair<Patch<String>, String>>, Double>> prioritize(List<Pair<File, Pair<Patch<String>, String>>> patches) {
        List<Pair<Pair<File, Pair<Patch<String>, String>>, Double>> result = this.patchPrioritizeAlgorithm.prioritize(patches);
        return result;
    }
}
