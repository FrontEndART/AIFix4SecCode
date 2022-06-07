package eu.assuremoss.utils;

import eu.assuremoss.framework.model.VulnerabilityEntry;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static eu.assuremoss.VulnRepairDriver.MLOG;
import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;
import static eu.assuremoss.utils.Configuration.descriptionPath;

/**
 * Class for creating statistics on the results
 */
public class Statistics {
    private final PathHandler path;

    public Statistics(PathHandler path) {
        this.path = path;
    }


    /**
     * Save information about detected vulnerabilities
     *
     * @param props
     * @param vulnEntries
     */
    public void saveVulnerabilityStatistics(Properties props, List<VulnerabilityEntry> vulnEntries) {
        saveFoundVulnerabilities(props, vulnEntries);
        saveVulnerabilityEntries(vulnEntries);
    }

    /**
     * Writes the summarized vulnerabilities into a text file
     * e.g.: Detected N vulnerabilities <br>
     *  - Xx EI_EXPOSE_REP <br>
     *  - Yx NP_NULL_ON_SOME_PATH <br>
     *  - Zx MS_SHOULD_BE_FINAL <br>
     *
     * @param props
     * @param vulnEntries the vulnerability entries that will be saved
     */
    public void saveFoundVulnerabilities(Properties props, List<VulnerabilityEntry> vulnEntries)  {
        Set<String> vulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toSet());
        List<String> allVulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toList());

        String fileName = "vuln_found.txt";
        String vulnStatsPath = String.valueOf(Paths.get(props.getProperty(RESULTS_PATH_KEY), "logs", fileName));

        try (Writer fileWriter = new FileWriter(vulnStatsPath)){
            fileWriter.write("Detected " + vulnEntries.size() + " vulnerabilities\n");
            for (String vulnType : vulnTypes) {
                // TODO use logger
                fileWriter.write(" - " + Collections.frequency(allVulnTypes, vulnType) + "x " + vulnType + "\n");
                //  System.out.println(" - " + Collections.frequency(allVulnTypes, vulnType) + "x " + vulnType + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Writes all vulnerability entries to a CSV file
     *
     * @param vulnEntries
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
     * Create statistics on the repaired vulnerabilities
     *
     * @param vulnEntries the vulnerability entries used in the statistics
     */
    public void createResultStatistics(List<VulnerabilityEntry> vulnEntries) {
        createRepairedVulnSum(vulnEntries, path.generatedPatches(), path.vulnFoundResult());
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
                MLOG.error("Generated patches were not found: " + patchesPath);
                return;
            }

            Set<String> vulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toSet());
            List<String> allVulnTypes = vulnEntries.stream().map(VulnerabilityEntry::getType).collect(Collectors.toList());

            writer.write("Detected " + vulnEntries.size() + " vulnerabilities\n");
            for (String vulnType : vulnTypes) {
                int foundVuln = Collections.frequency(allVulnTypes, vulnType);
                int repairedVuln = countRepairedVuln(directory.listFiles(), vulnType);
                String status = foundVuln == repairedVuln ? "✔" : "✖";
                writer.write(String.format(" - %s %d/%d %s\n", status, repairedVuln, foundVuln, vulnType));
            }

        } catch (IOException e) {
            MLOG.error("Statistics could not be created!");
            e.printStackTrace();
        }
    }

    /** Writes all vulnerability entries into a csv file with the result status.
     * Result status is based on whether the vulnerability has been fixed.
     *
     * @param vulnEntries
     * @param patchesPath
     */
    public void createVulnStatistic(List<VulnerabilityEntry> vulnEntries, String patchesPath) {
        File directory = new File(patchesPath);

        try (Writer writer = new FileWriter(path.vulnEntryStatistics())) {
            if (!directory.exists()) {
                MLOG.error("Generated patches were not found: " + patchesPath);
                return;
            }

            if (directory.listFiles() == null) {
                MLOG.fInfo("Patch files were not generated!");
                return;
            }

            writer.write(statisticsCSVHeader());
            for (int i = 0; i < vulnEntries.size(); i++) {
                VulnerabilityEntry vulnEntry = vulnEntries.get(i);
                String status = isPatchGeneratedForVuln(directory.listFiles(), vulnEntry) ? "✔" : "✖";
                writer.write(statisticsCSVFormat(i + 1, vulnEntry, status));
            }

        } catch (IOException e) {
            MLOG.error("Statistics could not be appended!");
            e.printStackTrace();
        }
    }

    /**
     * Counts how many of a given vulnerability are among the generated vulnerabilities.
     * The counting is performed on a Set of patches, therefore multiple patches for a vulnerability not counted.
     *
     * @param patchFiles - List of generated repair patches
     * @param vulnType - The vulnerability that we want to count
     * @return number of
     */
    private int countRepairedVuln(File[] patchFiles, String vulnType) {
        var patchesSet = Arrays.stream(patchFiles)
                .map(File::getName)
                .filter(patchName -> patchName.startsWith("patch_"))
                .map(patchName -> patchName.split("patch_\\d+_")[1])
                .collect(Collectors.toSet());

        return (int) patchesSet.stream().filter(patchName -> patchName.contains(vulnType + "_")).count();
    }

    private boolean isPatchGeneratedForVuln(File[] patchFiles, VulnerabilityEntry vulnEntry) {
        // patch_1_EI_EXPOSE_REP2_10_20_30_40.diff

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
        return "ID,Type,Status,Message\n";
    }

    private String statisticsCSVFormat(int id, VulnerabilityEntry vulnEntry, String status) {
        String message = "ok";

        if ("✖".equals(status) && vulnEntry.getStartCol() == -1 && vulnEntry.getEndCol() == -1)
            message = "Column info were not retrieved.";

        if ("✖".equals(status))
            message = "Probably build failed.";

        // Header: ID,Type,Status,Message
        return String.format("%s,%s,%s,%s\n", id, vulnEntry.getType(), status, escapeComma(message));
    }

    private String escapeComma(String str) {
        return str.replaceAll(",", " ");
    }

}
