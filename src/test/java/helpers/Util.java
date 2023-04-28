package helpers;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Util {
    public static void writeVulnerabilitiesToSER(List<VulnerabilityEntry> vulns, String output) throws IOException {
        FileOutputStream fos = new FileOutputStream(output);
        ObjectOutputStream objOutputStream = new ObjectOutputStream(fos);
        for (VulnerabilityEntry vuln : vulns) {
            objOutputStream.writeObject(vuln);
            objOutputStream.reset();
        }
        objOutputStream.close();
    }

    public static List<VulnerabilityEntry> readVulnerabilitiesFromSER(String inputPath) throws ClassNotFoundException, IOException {
        List<VulnerabilityEntry> vulns = new ArrayList();
        FileInputStream fis = new FileInputStream(inputPath);
        ObjectInputStream obj = new ObjectInputStream(fis);
        try {
            while (fis.available() != -1) {
                VulnerabilityEntry acc = (VulnerabilityEntry) obj.readObject();
                vulns.add(acc);
            }
        } catch (EOFException ex) {
//            MLOG.error(ex.toString());
        }
        return vulns;
    }

    public static String readFile(String path) throws IOException {
        return Files.readString(Paths.get(path)).replaceAll("\\n|\\r\\n", System.getProperty("line.separator"));
    }

    public static void cleanUpGeneratedTestFiles() throws IOException {
        FileUtils.cleanDirectory(new File(PathHelper.getActualResultsDir()));
    }

    public static List<List<String>> readCSVFirstNColumns(String path, int N) {
        List<List<String>> records = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(path))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                records.add(Arrays.asList(values).subList(0, values.length - N));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return records;
    }

}
