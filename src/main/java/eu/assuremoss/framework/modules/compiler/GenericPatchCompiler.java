package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.PatchCompiler;

import java.io.File;

public abstract class GenericPatchCompiler implements PatchCompiler {

    @Override
    public void revertPatch(Patch<String> patch, File srcLocation) {
    }

    @Override
    public void applyPatch(Patch<String> patch, File srcLocation) {
    }
}
