package helpers;

import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

public class VulnEntryHelper {


    public static List<VulnerabilityEntry> getVulnEntries() {
        var result = new ArrayList<VulnerabilityEntry>();

        String vulnEntriesPath = PathHelper.getVulnEntriesPath();

        try {
            FileInputStream fileIn = new FileInputStream(vulnEntriesPath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            result = (ArrayList<VulnerabilityEntry>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VulnerabilityEntry class not found");
            c.printStackTrace();
        }

        return result;
    }
}
