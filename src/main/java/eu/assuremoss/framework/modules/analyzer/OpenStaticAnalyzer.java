package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class OpenStaticAnalyzer implements CodeAnalyzer, VulnerabilityDetector, PatchValidator {

    private File OSAPath;

    public OpenStaticAnalyzer(String osaPath) {
        OSAPath = new File(osaPath);
    }

    @Override
    public List<CodeModel> analyzeSourceCode(File srcLocation) {
        List<CodeModel> resList = new ArrayList<>();

        // TODO: call SM toolchain to build the ASG and produce proper output files
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(new File(OSAPath, "pmd.txt")));
            pw.println("fake/path/A.java;1;2;3;4;PMD_XYZ;Vuln1");
            pw.println("fake/path/B.java;1;2;3;4;PMD_W;Vuln2");
            pw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(".")));
        return resList;
    }

    @Override
    public List<VulnerabilityEntry> getVulnerabilityLocations(File srcLocation) {
        List<VulnerabilityEntry> resList = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new FileReader(new File(OSAPath, "pmd.txt")));
            String line;
            while ((line = br.readLine()) != null) {
                VulnerabilityEntry ve = new VulnerabilityEntry();
                String[] lineParts = line.split(";");
                ve.setPath(lineParts[0]);
                ve.setStartLine(Integer.parseInt(lineParts[1]));
                ve.setStartCol(Integer.parseInt(lineParts[2]));
                ve.setEndLine(Integer.parseInt(lineParts[3]));
                ve.setEndCol(Integer.parseInt(lineParts[4]));
                ve.setType(lineParts[5]);
                resList.add(ve);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return resList;
    }

    @Override
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Patch<String> patch) {
        analyzeSourceCode(srcLocation);
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation);
        return !vulnerabilities.contains(ve);
    }
}
