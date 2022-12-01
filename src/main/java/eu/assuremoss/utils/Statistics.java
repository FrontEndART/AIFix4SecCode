package eu.assuremoss.utils;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;
import java.util.stream.Collectors;

import static eu.assuremoss.utils.Configuration.*;

/**
 * Class for creating statistics on the results
 */
public class Statistics {
    private static final Logger LOG = LogManager.getLogger(MavenPatchCompiler.class);
    public PathHandler path;

    public Statistics(PathHandler path) {
        this.path = path;
    }


    /**
     * Saves information about the detected vulnerabilities
     *
     * @param vulnEntries the vulnerability entries that will be saved
     */
    public void saveVulnerabilityStatistics(List<VulnerabilityEntry> vulnEntries) {
        saveFoundVulnerabilities(vulnEntries);
        saveVulnerabilityEntries(vulnEntries);
    }

    /**
     * Writes the summarized vulnerabilities to a text file
     * e.g.: Detected N vulnerabilities <br>
     *  - Xx EI_EXPOSE_REP <br>
     *  - Yx NP_NULL_ON_SOME_PATH <br>
     *  - Zx MS_SHOULD_BE_FINAL <br>
     *
     * @param vulnEntries the vulnerability entries that will be saved
     */
    public void saveFoundVulnerabilities(List<VulnerabilityEntry> vulnEntries)  {
        Set<String> vulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toSet());
        List<String> allVulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toList());

        try (Writer fileWriter = new FileWriter(path.vulnFound())){
            fileWriter.write("Detected " + vulnEntries.size() + " vulnerabilities\n");
            for (String vulnType : vulnTypes) {
                fileWriter.write(" - " + Collections.frequency(allVulnTypes, vulnType) + "x " + vulnType + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes all vulnerabilities to a CSV file
     *
     * @param vulnEntries the vulnerability entries that will be written to the file
     */
    public void saveVulnerabilityEntries(List<VulnerabilityEntry> vulnEntries) {
        try (Writer writer = new FileWriter(path.vulnEntries())){
            writer.write(vulnEntryCSVHeader());
            for (int i = 0; i < vulnEntries.size(); i++) {
                VulnerabilityEntry vulnEntry = vulnEntries.get(i);
                writer.write(vulnEntryCSVFormat(i + 1, vulnEntry));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String vulnEntryCSVHeader() {
        return "ID,Type,VulnType,Variable,StartLine,EndLine,StartCol,EndCol,Path,Description\n";
    }

    private String vulnEntryCSVFormat(int id, VulnerabilityEntry vulnEntry) {
        return String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n", id, vulnEntry.getType(),
                vulnEntry.getVulnType(), vulnEntry.getVariable(), vulnEntry.getStartLine(), vulnEntry.getEndLine(),
                vulnEntry.getStartCol(), vulnEntry.getEndCol(), escapeComma(vulnEntry.getPath()), escapeComma(vulnEntry.getDescription()));
    }

    /**
     * Creates statistics on the repaired vulnerabilities
     *
     * @param vulnEntries the vulnerability entries used in the statistics
     */
    public void createResultStatistics(Properties props, List<VulnerabilityEntry> vulnEntries) {
        createRepairedVulnSum(vulnEntries, path.generatedPatches(), path.joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR , VULN_FOUND_RESULT_TXT));
        createVulnStatistic(vulnEntries, path.generatedPatches());
    }

    /** Sums up the number of repaired vulnerabilities,
     *  by comparing the found vulnerability entries with the generated patches. <br>
     *  e.g.: found: 3x EI_EXPOSE_REP2, generated 6x repair patches <br>
     *      -> ✔ 3/3 EI_EXPOSE_REP2
     *
     * @param vulnEntries list of the found vulnerabilities
     * @param patchesPath path of the generated patches
     * @param resultPath path of the created statistics file
     */
    public void createRepairedVulnSum(List<VulnerabilityEntry> vulnEntries, String patchesPath, String resultPath) {
        File directory = new File(patchesPath);

        try (Writer writer = new FileWriter(resultPath)) {
            if (!directory.exists()) {
                LOG.error("Generated patches were not found: " + patchesPath);
                return;
            }

            Set<String> vulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toSet());
            List<String> allVulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toList());

            writer.write("Detected " + vulnEntries.size() + " vulnerabilities\n");
            for (String vulnType : vulnTypes) {
                int foundVuln = Collections.frequency(allVulnTypes, vulnType);
                int repairedVuln = countRepairedVuln(directory.listFiles(), vulnType);
                String status = foundVuln == repairedVuln ? "OK" : "X";
                writer.write(String.format(" - %s %d/%d %s\n", status, repairedVuln, foundVuln, vulnType));
            }

        } catch (IOException e) {
            LOG.error("Statistics could not be created!");
            e.printStackTrace();
        }
    }

    /** Writes all vulnerability entries into a csv file with the result status.
     * Result status is based on whether the vulnerability has been fixed.
     *
     * @param vulnEntries list of the found vulnerabilities
     * @param patchesPath path of the generated patches
     */
    public void createVulnStatistic(List<VulnerabilityEntry> vulnEntries, String patchesPath) {
        File directory = new File(patchesPath);

        try (Writer writer = new FileWriter(path.vulnEntryStatistics())) {
            if (!directory.exists()) {
                LOG.error("Generated patches were not found: " + patchesPath);
                return;
            }

            if (directory.listFiles() == null) {
                LOG.error("Patch files were not generated!");
                return;
            }

            writer.write(statisticsCSVHeader());
            for (int i = 0; i < vulnEntries.size(); i++) {
                VulnerabilityEntry vulnEntry = vulnEntries.get(i);
                String status = isPatchGeneratedForVuln(directory.listFiles(), vulnEntry) ? "OK" : "X";
                writer.write(statisticsCSVFormat(i + 1, vulnEntry, status));
            }

        } catch (IOException e) {
            LOG.error("Statistics could not be appended!");
            e.printStackTrace();
        }
    }

    /**
     * Counts how many of a given vulnerability are among the generated vulnerabilities.
     * The counting is performed on a Set of patches, therefore multiple patches for a vulnerability not counted.
     *
     * @param patchFiles list of generated repair patches
     * @param vulnType the vulnerability that is counted
     * @return the number of different generated patches
     */
    private int countRepairedVuln(File[] patchFiles, String vulnType) {
        var patchesSet = Arrays.stream(patchFiles)
                .map(File::getName)
                .filter(patchName -> patchName.startsWith("patch_"))
                .map(patchName -> patchName.split("patch_\\d+_")[1])
                .collect(Collectors.toSet());

        return (int) patchesSet.stream().filter(patchName -> patchName.contains(vulnType + "_")).count();
    }

    /**
     * Checks if a patch were generated for a vulnerability
     *
     * @param patchFiles list of all the generated patch files
     * @param vulnEntry the vulnerability entry that we
     * @return true if the patch were found, else false
     */
    private boolean isPatchGeneratedForVuln(File[] patchFiles, VulnerabilityEntry vulnEntry) {
        for (File patchFile : patchFiles) {
            String[] patch = patchFile.getName().replaceAll(",", "").split("\\.")[0].split("_");

            // If patch formatted badly
            if (patch.length < 6) continue;

            int starLine = Integer.parseInt(patch[patch.length - 4]);
            int endLine = Integer.parseInt(patch[patch.length - 3]);
            int startCol = Integer.parseInt(patch[patch.length - 2]);
            int endCol = Integer.parseInt(patch[patch.length - 1]);

            if (isPatchMatchVulnEntry(vulnEntry, starLine, endLine, startCol, endCol)) return true;
        }

        return false;
    }

    private boolean isPatchMatchVulnEntry(VulnerabilityEntry vulnEntry, int starLine, int endLine, int startCol, int endCol) {
        return vulnEntry.getStartLine() == starLine && vulnEntry.getEndLine() == endLine &&
                vulnEntry.getStartCol() == startCol && vulnEntry.getEndCol() == endCol;
    }

    private String statisticsCSVHeader() {
        return "ID,Type,Status,Message,Generated,Filtered,Verified\n";
    }

    private String statisticsCSVFormat(int id, VulnerabilityEntry vulnEntry, String status) {
        String message = "ok";

        if ("✖".equals(status))
            message = "Build failure";

        if (vulnEntry.getGeneratedPatches() == 0)
            message = "No patches";

        if ("✖".equals(status) && vulnEntry.getStartCol() == -1 && vulnEntry.getEndCol() == -1)
            message = "Column info missing";


        // Header: ID,Type,Status,Message
        return String.format("%s,%s,%s,%s,%d,%d,%d\n", id, vulnEntry.getType(), status, escapeComma(message),
                vulnEntry.getGeneratedPatches(), vulnEntry.getFilteredPatches(), vulnEntry.getVerifiedPatches());
    }

    /**
     * Replace all commas with spaces for CSV writing
     * @param text the text that may contain commas
     * @return a text without commas
     */
    private String escapeComma(String text) {
        return text.replaceAll(",", " ");
    }

}
