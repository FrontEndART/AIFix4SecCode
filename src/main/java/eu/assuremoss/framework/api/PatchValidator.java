package eu.assuremoss.framework.api;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.File;

public interface PatchValidator {

    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Patch<String> patch);
}
