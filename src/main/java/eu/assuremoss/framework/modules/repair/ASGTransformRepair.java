package eu.assuremoss.framework.modules.repair;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;
import lombok.AllArgsConstructor;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ASGTransformRepair implements VulnerabilityRepairer {

    @Override
    public List<Pair<File, Patch<String>>> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels) {
        List<Pair<File, Patch<String>>> resList = new ArrayList<>();
        // for testing purposes load log4j file and create diff with a modified version in the resources folder
        File asyncAppenderOrig = new File(srcLocation, String.valueOf(Paths.get("main", "java", "org", "apache", "log4j", "AsyncAppender.java")));
        List<String> text1 = new ArrayList<>();
        List<String> text2 = new ArrayList<>();
        try {
            text1 = new BufferedReader(new FileReader(asyncAppenderOrig)).lines()
                    .collect(Collectors.toList());
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = loader.getResourceAsStream("AsyncAppender.java");
            text2 = new BufferedReader(new InputStreamReader(resourceStream)).lines()
                    .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //generating diff information.
        Patch<String> patch = DiffUtils.diff(text1, text2);
        System.out.println(patch.toString());
        resList.add(new Pair<>(asyncAppenderOrig, patch));

        return resList;
    }
}
