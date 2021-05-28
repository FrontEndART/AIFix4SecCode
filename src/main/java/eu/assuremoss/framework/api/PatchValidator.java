package eu.assuremoss.framework.api;

import eu.assuremoss.framework.model.Patch;
import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.File;

public interface PatchValidator {

    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Patch patch);
}
