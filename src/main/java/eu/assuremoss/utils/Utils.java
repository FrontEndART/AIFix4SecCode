package eu.assuremoss.utils;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Utils {
    private static final Logger LOG = LogManager.getLogger(Utils.class);

    public static void deleteIntermediatePatches(String patchSavePath) {
        FileFilter fileFilter = new WildcardFileFilter("repair_patch*.diff");
        File[] files = new File(patchSavePath).listFiles(fileFilter);
        for (File file : files) {
            try {
                LOG.info("Deleting " + file.getName());
                Files.delete(Path.of(file.getAbsolutePath()));
            } catch (IOException e) {
                LOG.error(e);
            }
        }
    }

    public static String getExtension() {
        String osName = System.getProperty("os.name");
        if (osName.contains("Windows")) {
            return ".exe";
        } else if (osName.contains("Linux")) {
            return "";
        }
        return "";
    }


}
