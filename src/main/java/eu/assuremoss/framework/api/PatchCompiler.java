package eu.assuremoss.framework.api;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.List;

public interface PatchCompiler {

    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies);

    public List<Pair<File, Pair<Patch<String>, String>>> applyAndCompile(File srcLocation, List<Pair<File, Pair<Patch<String>, String>>> patches, boolean runTests);

    public void revertPatch(Pair<File, Patch<String>> patch, File srcLocation);

    public void applyPatch(Pair<File, Patch<String>> patch, File srcLocation);

    public String getBuildDirectoryName();
}
