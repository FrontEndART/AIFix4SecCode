package eu.assuremoss.utils;

import java.nio.file.Paths;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;

public class PathHandler {
    private Properties props;

    // Directories
    private final String patchesDir = "patches";
    private final String logsDir = "logs";
    private final String buildLogsDir = "build_logs";

    // Files
    private final String vulnFound = "vuln_found.txt";
    private final String vulnFoundResult = "vuln_found_result.txt";
    private final String vulnEntries = "vuln_entries.csv";
    private final String vulnEntriesResult = "vuln_entries_result.csv";
    private final String vulnBuildFile = "vuln_?.txt";
    private final String logFinish = "log_finish.txt";
    private final String patchUnitTestsCSV = "patch_unit_tests.csv";


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
        return path.startsWith(Utils.getWorkingDir());
    }

    // Directories

    public String logsDir() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir);
    }

    public String buildDir() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, buildLogsDir);
    }

    public String generatedPatches() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), patchesDir);
    }

    // Files
    
    public String vulnFound() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, vulnFound);
    }

    public String vulnEntries() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, vulnEntries);
    }

    public String vulnFoundResult() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, vulnFoundResult);
    }

    public String vulnEntryStatistics() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, vulnEntriesResult);
    }

    public String vulnBuildLogFile(int vulnIndex) {
        return joinPath(buildDir(), vulnBuildFile.replace("?", String.valueOf(vulnIndex)));
    }

    public String logFinishFile() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), logsDir, logFinish);
    }

    public String patchUnitTests() {
        return joinPath(logsDir(), patchUnitTestsCSV);
    }
}
