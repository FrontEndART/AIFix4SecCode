package eu.assuremoss.utils;

import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

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

    public static Map<String, String> getWarningMappingFromProp(Properties properties) {
        Map<String, String> warningMap = new HashMap<>();
        List<String> mappingPropertyNames = properties.keySet().stream().filter(p -> p.toString().startsWith("mapping")).map(pObj -> pObj.toString()).collect(Collectors.toList());
        for(String mappingKey : mappingPropertyNames) {
            warningMap.put(mappingKey.replaceFirst("^mapping.", ""), properties.getProperty(mappingKey));
        }

        return  warningMap;
    }

    public static void archiveResults(String patchSavePath, String archivePath, String descriptionPath, String currentTime) {
        File src = new File(patchSavePath);
        File dest = new File(archivePath);
        File desc = new File(descriptionPath + File.separator + "description.xml");

        try {
            if (!dest.exists()) {
                Path path = Paths.get(dest.toString());
                Files.createDirectory(path);
            }
            FileOutputStream fos = new FileOutputStream(dest + File.separator + currentTime + ".zip");
            ZipOutputStream zos = new ZipOutputStream(fos);

            File[] files = src.listFiles();
            for (File fileToZip : files) {
                FileInputStream fis = new FileInputStream(fileToZip);
                ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
                zos.putNextEntry(zipEntry);

                byte[] bytes = new byte[1024];
                int length;
                while ((length = fis.read(bytes)) >= 0) {
                    zos.write(bytes, 0, length);
                }

                zos.closeEntry();
                fis.close();
            }
            FileInputStream fis = new FileInputStream(desc);
            ZipEntry zipEntry = new ZipEntry(desc.getName());
            zos.putNextEntry(zipEntry);

            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }

            zos.closeEntry();
            fis.close();

            zos.close();
            fos.close();

        } catch (IOException e) {
            LOG.error("Archive was unsuccessful.");
            LOG.error(e);
        }
    }
}
