package eu.assuremoss.utils;

import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class PatchDeleter {
    private static final Logger LOG = LogManager.getLogger(PatchDeleter.class);

    public static void deletePatches(String patchSavePath, int patchCount) {
        for (int i = 1; i < patchCount; i++) {
            try {
                String patchPath = String.valueOf(Path.of(patchSavePath, "patch" + i + ".diff"));
                LOG.info("Deleting " + patchPath);
                Files.delete(Path.of(patchPath));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
