package eu.assuremoss.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestInfoExtractor {
    public static int patch = 0;
    public static String fileName = "";

    /**
     * @return the header of the CSV
     */
    public static String getUnitTestHeaderCSV() {
        return "Vuln_ID,Patch,Tests run,Failures,Errors,Skipped\n";
    }

    /**
     * Extract information from the result unit test text, and
     * performs the conversion to CSV format corresponding to the header.
     * @param logFileName name of the vulnerability build log file
     * @param line the line which contains the unit test result
     * @return a row of the CSV
     */
    public static String getUnitTestRowCSV(String logFileName, String line) {
        String testsRun = getValue(line, "Tests run: ([0-9]+)");
        String failures = getValue(line, "Failures: ([0-9]+)");
        String errors = getValue(line, "Errors: ([0-9]+)");
        String skipped = getValue(line, "Skipped: ([0-9]+)");

        return String.format("%s,%s,%s,%s,%s,%s\n", getPatchName(logFileName), patch, testsRun, failures, errors, skipped);
    }

    /**
     * Used for extracting numbers from the unit test result
     */
    public static String getValue(String text, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }

    /**
     * Returns a unique identifier for each compiled patch <br>
     * e.g. vuln_2_3 - third generated patch for the second vulnerability
     * @param logFileName patch build log name
     * @return name of the patch
     */
    public static String getPatchName(String logFileName) {
        // TODO: read log.txt fileName from pathHandler
        if ("log.txt".equals(logFileName)) return "build";

        if (logFileName.equals(fileName)) {
            patch++;
        } else {
            fileName = logFileName;
            patch = 1;
        }

        return logFileName.split("\\.")[0];
    }
}
