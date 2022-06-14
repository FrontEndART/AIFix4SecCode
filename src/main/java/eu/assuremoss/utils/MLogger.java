package eu.assuremoss.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.regex.Pattern;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;

public class MLogger {
    private Writer fileWriter;
    private Writer unitTestInfoWriter;
    private String logFileName;
    private String logFilePath;
    private PathHandler path;

    public MLogger(Properties props, String logFileName, PathHandler path) throws IOException {
        this.logFileName = logFileName;
        this.logFilePath = logFilePath(props);
        this.fileWriter = new FileWriter(logFilePath);
        this.path = path;
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
        try {
            fileWriter.write(message);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String logFilePath(Properties props) {
        return String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), "logs", logFileName));
    }

    public void openFile(boolean append) {
        try {
            this.fileWriter = new FileWriter(logFilePath, append);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void openFile() {
        openFile(false);
    }

    public void closeFile() {
        try {
            this.fileWriter.close();
            this.fileWriter = null;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void changeOutPutFile(String newFile) {
        closeFile();

        this.logFileName = Paths.get(newFile).getFileName().toString();
        this.logFilePath = newFile;

        openFile();
    }

    public void saveUnitTestInformation(String line) {
        String regex = "Tests run: [\\d]+, Failures: [\\d]+, Errors: [\\d]+, Skipped: [\\d]+";

        if (Pattern.matches(regex, line)) {
            // TODO: new singleton object: unitTestInfoWriter
            if (unitTestInfoWriter == null) {
                try {
                    unitTestInfoWriter = new FileWriter(path.patchUnitTests());
                    unitTestInfoWriter.write(TestInfoExtractor.getUnitTestHeaderCSV());
                } catch (IOException e) {
                    error("Could not open: " + path.patchUnitTests());
                }
            }

            try {
                unitTestInfoWriter.write(TestInfoExtractor.getUnitTestRowCSV(logFileName, line));
                unitTestInfoWriter.flush();
            } catch (IOException e) {
                error("Could not write to: " + path.patchUnitTests());
            }
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
