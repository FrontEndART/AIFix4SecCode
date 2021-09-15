package eu.assuremoss.framework.modules.analyzer;

import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.CodeAnalyzer;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;

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

        String[] command = new String[] {
                OSAPath.getPath(),
                "-resultsDir=" + VulnRepairDriver.getPatchSavePath(),
                "-projectName=" + VulnRepairDriver.getProjectName(),
                "-projectBaseDir=" + srcLocation,
                "-cleanResults=0",
                "-currentDate=0"
        };

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        try {
            Process process = processBuilder.start();
            BufferedReader out = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = out.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(
                VulnRepairDriver.getPatchSavePath(),
                VulnRepairDriver.getProjectName() + File.separator +
                        "java" + File.separator +
                        "0" + File.separator +
                        "openstaticanalyzer" + File.separator +
                        "asg"
        )));
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
    public boolean validatePatch(File srcLocation, VulnerabilityEntry ve, Pair<File, Patch<String>> patch) {
        analyzeSourceCode(srcLocation);
        List<VulnerabilityEntry> vulnerabilities = getVulnerabilityLocations(srcLocation);
        return !vulnerabilities.contains(ve);
    }
}
