package eu.assuremoss.utils;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;

public class MLogger {
    private final Writer fileWriter;
    public String logFileName;
    public String logFilePath;

    public MLogger(Properties props, String logFileName) throws IOException {
        this.logFileName = logFileName;
        this.logFilePath = logFilePath(props);
        this.fileWriter = new FileWriter(logFilePath);
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
        return String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), logFileName));
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
}
