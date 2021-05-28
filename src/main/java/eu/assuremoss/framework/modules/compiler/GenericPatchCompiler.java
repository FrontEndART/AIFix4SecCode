package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.model.Patch;

import java.io.File;

public abstract class GenericPatchCompiler implements PatchCompiler {

    @Override
    public void revertPatch(Patch patch, File srcLocation) {
    }

    @Override
    public void applyPatch(Patch patch, File srcLocation) {
    }
}
