package eu.assuremoss.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ProcessRunner {

    public static void run(ProcessBuilder processBuilder) {
        MLogger MLOG = MLogger.getActiveLogger();
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                MLOG.fInfo(line);
                MLOG.saveUnitTestInformation(line);
            }
        } catch (IOException e) {
            MLOG.info(String.valueOf(e));
        }
    }

    public static String runAndReturnMessage(ProcessBuilder processBuilder) {
        MLogger MLOG = MLogger.getActiveLogger();
        StringBuilder message = new StringBuilder();

        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                MLOG.fInfo(line);
                MLOG.saveUnitTestInformation(line);
                message.append(line);
            }
        } catch (IOException e) {
            MLOG.info(String.valueOf(e));
        }

        return message.toString();
    }
}
