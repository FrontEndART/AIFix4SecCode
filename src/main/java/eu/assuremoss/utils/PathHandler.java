package eu.assuremoss.utils;

import java.nio.file.Paths;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;

public class PathHandler {
    private Properties props;

    // Files
    private final String vulnEntries = "vuln_entries.csv";
    private final String vulnEntriesResult = "vuln_entries_result.csv";
    private final String vulnFoundResult = "vuln_found_result.txt";

    // Directories
    private final String patchesDir = "patches";
    private final String logsDir = "logs";

    public static String joinPath(String first, String ...args) {
        return String.valueOf(Paths.get(first, args));
    }

    public PathHandler(Properties props) {
        this.props = props;
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

    public String generatedPatches() {
        return joinPath(props.getProperty(RESULTS_PATH_KEY), patchesDir);
    }
}
