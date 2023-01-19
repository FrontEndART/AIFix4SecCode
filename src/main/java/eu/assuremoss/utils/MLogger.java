package eu.assuremoss.utils;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;

public class MLogger {
    public static MLogger activeLogger = null;
    private PrintStream fileWriter;
    private PrintStream unitTestInfoWriter;
    private String logFileDir;
    private String logFileName;
    public String logFilePath;
    private final String unitTestsPathes;


    public MLogger(String logFileName, PathHandler path, boolean isTestingEnabled) throws IOException {
        this.logFileName = logFileName;
        //this.logFilePath = logFilePath(Paths.get(resultDir/*props.getProperty(RESULTS_PATH_KEY))*/));
        this.logFilePath = String.valueOf(Paths.get(path.getResultsPath(), "logs", logFileName));
        this.fileWriter = new PrintStream(logFilePath);
        if (isTestingEnabled/*Configuration.isTestingEnabled(props)*/)
           unitTestsPathes = path.patchUnitTests();
        else unitTestsPathes = null;
        activeLogger = this;
    }

    public MLogger(String file) {
        unitTestsPathes = null;
        logFileName = null;
        logFilePath = null;
        try {
            this.fileWriter = new PrintStream(file);
        } catch (IOException e) {
            e.printStackTrace();
        }
        activeLogger = this;
    }

    public static void setActiveLogger(MLogger active) {
        activeLogger = active;
    }

    public static MLogger getActiveLogger() {
        activeLogger.flush();
        return activeLogger;
    }

    public PrintStream getFileWriter() {
        return fileWriter;
    }

    public void flush() {
        fileWriter.flush();
    }

    public void info(String message) {
        String formattedMessage = String.format("[%s] %s\n", timestamp(), message);
        System.out.print(formattedMessage);
        logIntoFile(formattedMessage);
    }

    public void ninfo(String message) {
        String formattedMessage = String.format("\n[%s] %s\n", timestamp(), message);
        System.out.print(formattedMessage);
        logIntoFile(formattedMessage);
    }

    public void error(String message) {
        String formattedMessage = String.format("[%s] ERROR %s\n", timestamp(), message);
        System.out.print(formattedMessage);
        logIntoFile(formattedMessage);
    }

    public void fInfo(String message) {
        String formattedMessage = String.format("[%s] %s\n", timestamp(), message);
        logIntoFile(String.valueOf(formattedMessage));
    }

    private void logIntoFile(String message) {
        fileWriter.print(message);
        fileWriter.flush();
    }

    /*public String logFilePath(Path resultsDir) {
        return String.valueOf(resultsDir, "logs", logFileName);
    }*/

    public void closeFile() {
        this.fileWriter.close();
        this.fileWriter = null;
    }

    public void saveUnitTestInformation(String line) {
        if (unitTestsPathes == null) return;

        String regex = "Tests run: [\\d]+, Failures: [\\d]+, Errors: [\\d]+, Skipped: [\\d]+";

        if (Pattern.matches(regex, line)) {
            if (unitTestInfoWriter == null) {
                try {
                    unitTestInfoWriter = new PrintStream(unitTestsPathes);
                    unitTestInfoWriter.println(TestInfoExtractor.getUnitTestHeaderCSV());
                } catch (IOException e) {
                    error("Could not open: " + unitTestsPathes);
                }
            }

            
            unitTestInfoWriter.println(TestInfoExtractor.getUnitTestRowCSV(logFileName, line));
            unitTestInfoWriter.flush();
            
        }
    }

    /**
     * Returns the current date in the following format: yyyy/MM/dd HH:mm:ss <br />
     * e.g: 2022/01/01 12:00:00
     * @return current date
     */
    public String timestamp() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
        Date date = new Date();
        return formatter.format(date);
    }


    // Getters
    public String getLogFilePath() {
        return logFilePath;
    }
}
