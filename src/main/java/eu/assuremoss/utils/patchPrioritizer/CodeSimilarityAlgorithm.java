package eu.assuremoss.utils.patchPrioritizer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public class CodeSimilarityAlgorithm implements PatchPrioritizeAlgorithm {

    @Override
    public void prioritize(List<Pair<File, Pair<Patch<String>, String>>> patches) {
        // TODO: Implement SLAB's simple patch prioritization algorithm based on code similarity.
    }
}
