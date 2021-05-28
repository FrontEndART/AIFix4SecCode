package eu.assuremoss.framework.modules.repair;

import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.Patch;
import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ASGTransformRepair implements VulnerabilityRepairer {

    @Override
    public List<Patch> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels) {
        List<Patch> resList = new ArrayList<>();
        Patch patch = new Patch();
        patch.setSrcRootFolder(srcLocation.getAbsolutePath());
        patch.setChangedFiles(new HashMap<>());
        patch.getChangedFiles().put(new File(ve.getPath()), "+++ fake change");
        patch.setUniDiff("+++ fake change\n--- more fake changes");
        resList.add(patch);
        return resList;
    }
}
