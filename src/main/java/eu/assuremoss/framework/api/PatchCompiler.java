package eu.assuremoss.framework.api;

import eu.assuremoss.framework.model.Patch;

import java.io.File;
import java.util.List;

public interface PatchCompiler {

    public List<Patch> applyAndCompile(File srcLocation, List<Patch> patches, boolean runTests);

    public void revertPatch(Patch patch, File srcLocation);

    public void applyPatch(Patch patch, File srcLocation);
}
