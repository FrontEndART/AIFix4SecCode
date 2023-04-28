package eu.assuremoss.utils;

import java.nio.file.Paths;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.*;

public class PathHandler {
    //private Properties props;
    private String resultsPath;
    private String validationResultsPath;

    /*public PathHandler(Properties props) {
        this.props = props;
    }*/

    public PathHandler(String resultsPath) {
        this.resultsPath = resultsPath;
        this.validationResultsPath = resultsPath;
    }

    public PathHandler(String resultsPath, String validationResultsPath) {
        this.resultsPath = resultsPath;
        this.validationResultsPath = validationResultsPath;
    }

    public String getResultsPath() {
        return resultsPath;
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
        return joinPath(resultsPath, LOGS_DIR);
    }

    public String buildDir() {
        return joinPath(resultsPath, LOGS_DIR, BUILD_LOGS_DIR);
    }

    public String generatedPatches() {
        return joinPath(resultsPath, PATCHES_DIR);
    }

    public String asgDir() {
        return joinPath(resultsPath, ASG_DIR);
    }

    // Files
    
    public String vulnFound() {
        return joinPath(resultsPath, LOGS_DIR, VULN_FOUND_TXT);
    }

    public String vulnEntries() {
        return joinPath(resultsPath, LOGS_DIR, VULN_ENTRIES_CSV);
    }

    public String vulnFoundResult() {
        return joinPath(resultsPath, LOGS_DIR, VULN_FOUND_RESULT_TXT);
    }

    public String vulnEntryStatistics() {
        return joinPath(resultsPath, LOGS_DIR, VULN_ENTRIES_RESULT_CSV);
    }

    public String vulnBuildLogFile(int vulnIndex) {
        return joinPath(buildDir(), VULN_TXT.replace("?", String.valueOf(vulnIndex)));
    }

    public String spotbugsXML(boolean isValidation) {
        if (!isValidation)
            return joinPath(resultsPath, Configuration.SPOTBUGS_RESULTFILE);
        return joinPath(validationResultsPath, Configuration.SPOTBUGS_RESULTFILE);
    }

    public String patchUnitTests() {
        return joinPath(logsDir(), PATCH_UNIT_TESTS_CSV);
    }
}
