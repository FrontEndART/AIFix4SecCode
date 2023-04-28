package eu.assuremoss.utils.patchPrioritizer;


import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public class PatchPrioritizer {
    private final PatchPrioritizeAlgorithm patchPrioritizeAlgorithm;

    public PatchPrioritizer(PatchPrioritizeAlgorithm patchPrioritizeAlgorithm) {
        this.patchPrioritizeAlgorithm = patchPrioritizeAlgorithm;
    }

    public void prioritize(List<Pair<File, Pair<Patch<String>, String>>> patches) {
        this.patchPrioritizeAlgorithm.prioritize(patches);
    }
}
