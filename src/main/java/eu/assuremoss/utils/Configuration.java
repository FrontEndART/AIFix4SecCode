package eu.assuremoss.utils;

import eu.assuremoss.VulnRepairDriver;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Properties;


/**
 * Class for configuring the main program
 */
public class Configuration {
    private static final Logger LOG = LogManager.getLogger(Configuration.class);

    public static final String DEFAULT_CONFIG_FILE_NAME = "config.properties";
    public static final String DEFAULT_MAPPING_FILE_NAME = "mapping.properties";
    public static final String PROJECT_NAME_KEY = "config.project_name";
    public static final String PROJECT_PATH_KEY = "config.project_path";
    public static final String PROJECT_SOURCE_PATH_KEY = "config.project_source_path";
    public static final String PROJECT_BUILD_TOOL_KEY = "config.project_build_tool";
    public static final String PROJECT_RUN_TESTS = "config.project_run_tests";
    public static final String OSA_PATH_KEY = "config.osa_path";
    public static final String OSA_EDITION_KEY = "config.osa_edition";
    public static final String RESULTS_PATH_KEY = "config.results_path";
    public static final String VALIDATION_RESULTS_PATH_KEY = "config.validation_results_path";
    public static final String ARCHIVE_PATH = "config.archive_path";
    public static final String ARCHIVE_ENABLED = "config.archive_enabled";

    public Properties properties;
    private final ClassLoader loader;

    /**
     * @param generalFileName general configuration file/resource path
     * @param mapFileName mapping configuration file/resource path
     * @throws IOException Thrown when the file/resource doesn't exist
     */
    public Configuration(String generalFileName, String mapFileName) throws IOException {
        loader = Thread.currentThread().getContextClassLoader();
        properties = new Properties();
        loadConfiguration(generalFileName);
        loadConfiguration(mapFileName);

        convertRelativePathToAbsolutePath();
    }

    private void loadConfiguration(String confFileName) throws IOException {
        URL resource = loader.getResource(confFileName);
        if (resource == null) {
            properties.putAll(loadPropertiesFromFile(confFileName));
        } else {
            properties.putAll(loadPropertiesFromResource(confFileName));
        }
        LOG.info("Successfully loaded " + confFileName);
    }

    /**
     * @param fileName The configuration file path
     * @throws IOException Thrown when the file doesn't exist
     */
    public Properties loadPropertiesFromFile(String fileName) throws IOException {
        Properties prop = new Properties();
        try (InputStream stream = new FileInputStream(fileName)) {
            prop.load(stream);
        }

        return prop;
    }

    /**
     * @param fileName The configuration resource path
     * @throws IOException Thrown when the resource doesn't exist
     */
    private Properties loadPropertiesFromResource(String fileName) throws IOException {
        Properties prop = new Properties();
        try (InputStream stream = loader.getResourceAsStream(fileName)) {
            prop.load(stream);
        }

        return prop;
    }

    /**
     * Converts the problematic relative path to absolute path (PROJECT_PATH)
     */
    private void convertRelativePathToAbsolutePath() {
        updatePathToAbsolute(PROJECT_PATH_KEY);
    }

    public static boolean archiveEnabled(Properties props) {
        return Boolean.parseBoolean(props.getProperty(ARCHIVE_ENABLED));
    }

    public static String descriptionPath(Properties props) {
        return String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), "osa_xml"));
    }

    public static String patchSavePath(Properties props) {
        return String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), "patches"));
    }

    public static boolean isTestingEnabled(Properties props) {
        return Boolean.parseBoolean(props.getProperty(PROJECT_RUN_TESTS));
    }

    private void updatePathToAbsolute(String key) {
        String path = properties.get(key).toString();

        if (PathHandler.isAbsolute(path)) {
            return;
        }

        properties.setProperty(key, PathHandler.toAbsolute(path));
    }

}
