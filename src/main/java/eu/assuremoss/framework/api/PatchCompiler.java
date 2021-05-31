package eu.assuremoss.framework.api;

import com.github.difflib.patch.Patch;

import java.io.File;
import java.util.List;

public interface PatchCompiler {

    public List<Patch<String>> applyAndCompile(File srcLocation, List<Patch<String>> patches, boolean runTests);

    public void revertPatch(Patch<String> patch, File srcLocation);

    public void applyPatch(Patch<String> patch, File srcLocation);
}
