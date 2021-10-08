package eu.assuremoss.framework.api;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public interface PatchCompiler {

    public List<Pair<File, Patch<String>>> applyAndCompile(File srcLocation, List<Pair<File, Patch<String>>> patches, boolean runTests);

    public void revertPatch(Pair<File, Patch<String>> patch, File srcLocation);

    public void applyPatch(Pair<File, Patch<String>> patch, File srcLocation);
}
