package eu.assuremoss.framework.modules.repair;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class ASGTransformRepair implements VulnerabilityRepairer {

    @Override
    public List<Pair<File, Patch<String>>> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels) {
        List<Pair<File, Patch<String>>> resList = new ArrayList<>();
        List<String> text1=Arrays.asList("this is a test","a test");
        List<String> text2= Arrays.asList("this is a testfile","a test");

        //generating diff information.
        Patch<String> patch = DiffUtils.diff(text1, text2);
        resList.add(new Pair<>(new File("test"), patch));

        return resList;
    }
}
