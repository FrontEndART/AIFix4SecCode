package helpers;

import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.PathHandler;

import java.io.IOException;
import java.nio.file.Paths;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;
import static eu.assuremoss.utils.Configuration.VALIDATION_RESULTS_PATH_KEY;

public class PathHelper {
    public static final String mockedResultsPath = String.valueOf(Paths.get("src","test", "resources", "mocked-results"));
    public static final String testResultsPath = String.valueOf(Paths.get("src","test", "results"));

    public static PathHandler getPathHandler() throws IOException {
        Configuration config = new Configuration("config.properties", "mapping.properties");

        return new PathHandler(config.properties.getProperty(RESULTS_PATH_KEY), config.properties.getProperty(VALIDATION_RESULTS_PATH_KEY));
    }

    public static String getVulnEntriesPath() {
        return PathHandler.joinPath(PathHelper.mockedResultsPath, "vulns.ser");
    }

    public static String getActualResultsDir() {
        return PathHandler.joinPath(PathHelper.testResultsPath, "actual");
    }

    public static String getExpectedResultsDir() {
        return PathHandler.joinPath(PathHelper.testResultsPath, "expected");
    }

    public static String getPatchesPath() {
        return PathHandler.joinPath(PathHelper.testResultsPath, "patches");
    }



    public static String getLogFilePath() {
        return PathHandler.joinPath(PathHelper.testResultsPath, "logs", "log.txt");
    }
}
