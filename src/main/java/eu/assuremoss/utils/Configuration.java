package eu.assuremoss.utils;

import eu.assuremoss.VulnRepairDriver;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

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
    public static final String PROJECT_NAME_KEY = "config.project_name";
    public static final String PROJECT_PATH_KEY = "config.project_path";
    public static final String PROJECT_BUILD_TOOL_KEY = "config.project_build_tool";
    public static final String OSA_PATH_KEY = "config.osa_path";
    public static final String OSA_EDITION_KEY = "config.osa_edition";
    public static final String RESULTS_PATH_KEY = "config.results_path";
    public static final String VALIDATION_RESULTS_PATH_KEY = "config.validation_results_path";
    public static final String ARCHIVE_PATH = "config.archive_path";
    public static final String ARCHIVE_ENABLED = "config.archive_enabled";

    public Properties properties;
    private final ClassLoader loader;

    /**
     * @param fileName The configuration file/resource path
     * @throws IOException Thrown when the file/resource doesn't exist
     */
    public Configuration(String fileName) throws IOException {
        properties = new Properties();
        loader = Thread.currentThread().getContextClassLoader();

        URL resource = loader.getResource(fileName);

        if (resource == null) {
            loadPropertiesFromFile(fileName);
            return;
        }

        loadPropertiesFromResource(fileName);

        LOG.info("Successfully loaded configuration properties.");
    }

    /**
     * @param fileName The configuration file path
     * @throws IOException Thrown when the file doesn't exist
     */
    private void loadPropertiesFromFile(String fileName) throws IOException {
        try (InputStream stream = new FileInputStream(fileName)) {
            properties.load(stream);
        }
    }

    /**
     * @param fileName The configuration resource path
     * @throws IOException Thrown when the resource doesn't exist
     */
    private void loadPropertiesFromResource(String fileName) throws IOException {
        try (InputStream stream = loader.getResourceAsStream(fileName)) {
            properties.load(stream);
        }
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
}
