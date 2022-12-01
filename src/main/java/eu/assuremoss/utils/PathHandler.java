package eu.assuremoss.utils;

import java.nio.file.Paths;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.*;

public class PathHandler {
    private Properties props;

    public PathHandler(Properties props) {
        this.props = props;
    }

    public static String joinPath(String first, String... args) {
        return String.valueOf(Paths.get(first, args));
    }

    public static String toAbsolute(String path) {
        return joinPath(Utils.getWorkingDir(), path);
    }

    public static boolean isAbsolute(String path) {
        return Paths.get(path).isAbsolute();
    }

    // Directories

    public String logsDir() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR);
    }

    public String buildDir() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, BUILD_LOGS_DIR);
    }

    public String generatedPatches() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), PATCHES_DIR);
    }

    public String asgDir() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), ASG_DIR);
    }

    // Files
    
    public String vulnFound() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, VULN_FOUND_TXT);
    }

    public String vulnEntries() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, VULN_ENTRIES_CSV);
    }

    public String vulnFoundResult() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, VULN_FOUND_RESULT_TXT);
    }

    public String vulnEntryStatistics() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, VULN_ENTRIES_RESULT_CSV);
    }

    public String vulnBuildLogFile(int vulnIndex) {
        return joinPath(buildDir(), VULN_TXT.replace("?", String.valueOf(vulnIndex)));
    }

    public String spotbugsXML(boolean isValidation) {
        if (!isValidation)
            return joinPath(props.getProperty(RESULTS_PATH_KEY), Configuration.SPOTBUGS_RESULTFILE);
        return joinPath(props.getProperty(VALIDATION_RESULTS_PATH_KEY), Configuration.SPOTBUGS_RESULTFILE);
    }

    public String logFile() {
        return LOG_TXT;
    }

    public String patchUnitTests() {
        return joinPath(logsDir(), PATCH_UNIT_TESTS_CSV);
    }
}
