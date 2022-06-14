package eu.assuremoss.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TestInfoExtractor {

    public static String getUnitTestHeaderCSV() {
        return "Patch,Tests run,Failures,Errors,Skipped\n";
    }

    public static String getUnitTestRowCSV(String logFileName, String line) {
        String testsRun = getValue(line, "Tests run: ([0-9]+)");
        String failures = getValue(line, "Failures: ([0-9]+)");
        String errors = getValue(line, "Errors: ([0-9]+)");
        String skipped = getValue(line, "Skipped: ([0-9]+)");

        return String.format("%s,%s,%s,%s,%s\n", logFileName, testsRun, failures, errors, skipped);
    }

    public static String getValue(String text, String key) {
        Pattern pattern = Pattern.compile(key);
        Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1);
        }

        return "";
    }
}
