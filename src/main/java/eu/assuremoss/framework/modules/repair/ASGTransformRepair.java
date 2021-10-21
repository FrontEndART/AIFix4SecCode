package eu.assuremoss.framework.modules.repair;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ASGTransformRepair implements VulnerabilityRepairer {

    @Override
    public List<Pair<File, Patch<String>>> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels) {
        List<Pair<File, Patch<String>>> resList = new ArrayList<>();
        // for testing purposes load two modifications of the test project and generate a valid and a wrong patch
        File asyncAppenderOrig = new File(srcLocation, String.valueOf(Paths.get("src", "main", "java", "vir", "samples", "HelloWorld.java")));
        List<String> text1 = new ArrayList<>();
        List<String> text2 = new ArrayList<>();
        List<String> text3 = new ArrayList<>();
        try {
            text1 = new BufferedReader(new FileReader(asyncAppenderOrig)).lines()
                    .collect(Collectors.toList());
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = loader.getResourceAsStream("HelloWorld-good.java");
            text2 = new BufferedReader(new InputStreamReader(resourceStream)).lines()
                    .collect(Collectors.toList());
            resourceStream = loader.getResourceAsStream("HelloWorld-bad.java");
            text3 = new BufferedReader(new InputStreamReader(resourceStream)).lines()
                    .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //generating diff information.
        Patch<String> patchGood = DiffUtils.diff(text1, text2);
        Patch<String> patchBad = DiffUtils.diff(text1, text3);

        resList.add(new Pair<>(asyncAppenderOrig, patchGood));
        resList.add(new Pair<>(asyncAppenderOrig, patchBad));

        return resList;
    }
}
