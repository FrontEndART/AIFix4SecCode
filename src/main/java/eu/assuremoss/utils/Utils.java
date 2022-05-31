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

import static eu.assuremoss.VulnRepairDriver.MLOG;
import static eu.assuremoss.utils.Configuration.*;
import static eu.assuremoss.utils.Configuration.patchSavePath;

public class Utils {
    private static final Logger LOG = LogManager.getLogger(Utils.class);

    public static void deleteIntermediatePatches(String patchSavePath) {
        FileFilter fileFilter = new WildcardFileFilter("repair_patch*.diff");
        File[] files = new File(patchSavePath).listFiles(fileFilter);
        for (File file : files) {
            try {
                MLOG.fInfo("Deleting" + file.getName());
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
        List<String> mappingPropertyNames = properties.keySet().stream().filter(p -> p.toString().startsWith("mapping.")).map(pObj -> pObj.toString()).collect(Collectors.toList());
        for(String mappingKey : mappingPropertyNames) {
            warningMap.put(mappingKey.replaceFirst("^mapping.", ""), properties.getProperty(mappingKey));
        }

        return  warningMap;
    }

    public static Map<String, Map<String, String>> getFixStrategies(Properties properties) {
        Map<String, Map<String, String>> fixStrategies = new HashMap<>();
        List<String> strategyPropertyNames = properties.keySet().stream().filter(p -> p.toString().startsWith("strategy.")).map(pObj -> pObj.toString()).collect(Collectors.toList());
        for(String strategyKey : strategyPropertyNames) {
            String fixStrategyKey = strategyKey.replaceFirst("^strategy.", "");
            String[] strategies = properties.getProperty(strategyKey).split("\\|");
            Map<String, String> fixStrategy = new HashMap<>();
            for(String strategy : strategies) {
                fixStrategy.put(strategy, properties.getProperty("desc." + strategy));
            }
            fixStrategies.put(fixStrategyKey, fixStrategy);
        }
        return fixStrategies;
    }

    public static void archiveResults(String patchSavePath, String archivePath, String descriptionPath, String currentTime) {
        File src = new File(patchSavePath);
        File dest = new File(archivePath);
        File desc = Paths.get(descriptionPath, "description.xml").toFile();

        if (!dest.exists()) {
            dest.mkdirs();
        }

        try(FileOutputStream fos = new FileOutputStream(new File(dest, currentTime + ".zip"));
            ZipOutputStream zos = new ZipOutputStream(fos)) {
            if (!dest.exists()) {
                Path path = Paths.get(dest.toString());
                Files.createDirectory(path);
            }

            File[] files = src.listFiles();
            for (File fileToZip : files) {
                archiveFile(zos, fileToZip);
            }
            archiveFile(zos, desc);

        } catch (IOException e) {
            LOG.error("Archive was unsuccessful.");
            LOG.error(e);
        }
    }

    private static void archiveFile(ZipOutputStream zos, File fileToZip) throws IOException {
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

    public static String getConfigFile(String[] args) {
        if (args.length > 0) {
            return args[1];
        }

        return DEFAULT_CONFIG_FILE_NAME;
    }

    public static String getMappingFile(String[] args) {
        if (args.length > 1) {
            return args[2];
        }

        return DEFAULT_MAPPING_FILE_NAME;
    }

    public static void createDirectoryForResults(Properties props) {
        try {
            Files.createDirectory(Paths.get(props.getProperty(RESULTS_PATH_KEY)));
        } catch (IOException e) {
            LOG.info("Unable to create results folder.");
        }
    }

    public static void createDirectoryForPatches(Properties props) {
        File patchSavePathDir = new File(patchSavePath(props));
        if (!patchSavePathDir.exists()) {
            try {
                Files.createDirectory(Paths.get(patchSavePath(props)));
            } catch (IOException e) {
                LOG.error("Failed to create directory for patches.");
            }
        }
    }

    public static void createDirectoryForValidation(Properties props) {
        File validationPathDir = new File(props.getProperty(VALIDATION_RESULTS_PATH_KEY));
        if (!validationPathDir.exists()) {
            try {
                Files.createDirectory(Paths.get(props.getProperty(VALIDATION_RESULTS_PATH_KEY)));
            } catch (IOException e) {
                LOG.error("Failed to create directory for validation.");
            }
        }
    }

    public static void createEmptyLogFile(Properties props) {
        String fileName = "log.txt";
        String path = String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), fileName));

        try {
            new FileWriter(path, false); //overwrites file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
