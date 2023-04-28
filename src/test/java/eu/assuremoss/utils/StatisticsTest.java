package eu.assuremoss.utils;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import helpers.PathHelper;
import helpers.Util;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static eu.assuremoss.utils.Configuration.*;

@Disabled
public class StatisticsTest {
    private static final String mockedResultsPath = String.valueOf(Paths.get("src", "test", "resources", "mocked-results"));
    private Statistics statistics;
    private List<VulnerabilityEntry> vulnEntries;

    @BeforeAll
    static void init() throws IOException {
        Files.createDirectories(Path.of(PathHelper.getActualResultsDir()));
    }

    @BeforeEach
    void setup() throws IOException, ClassNotFoundException {
        statistics = new Statistics(PathHelper.getPathHandler());
        statistics.path = Mockito.mock(PathHandler.class);

        vulnEntries = Util.readVulnerabilitiesFromSER(PathHelper.getVulnEntriesPath());

        Util.cleanUpGeneratedTestFiles();
    }

    @AfterEach
    void tearDown() throws IOException {
        Util.cleanUpGeneratedTestFiles();
    }

    @Test
    public void shouldSaveFoundVulnerabilities() throws IOException {
        final String actual_vulnFoundPath = PathHandler.joinPath(mockedResultsPath, VULN_FOUND_TXT);
        final String expected_vulnFoundPath = PathHandler.joinPath(mockedResultsPath, VULN_FOUND_TXT);

        //joinPath(props.getProperty(RESULTS_PATH_KEY), LOGS_DIR, VULN_FOUND_TXT);
        Mockito.when(statistics.path.vulnFound()).thenReturn(actual_vulnFoundPath);
        Assertions.assertEquals(actual_vulnFoundPath, statistics.path.vulnFound());

        statistics.saveFoundVulnerabilities(vulnEntries);

        Assertions.assertEquals(Util.readFile(expected_vulnFoundPath), Util.readFile(actual_vulnFoundPath));
    }

    @Test
    public void shouldSaveVulnerabilityEntries() throws IOException {
        final String actual_vulnEntriesPath = PathHandler.joinPath(PathHelper.getActualResultsDir(), "vuln_entries.csv");
        final String expected_vulnEntriesPath = PathHandler.joinPath(PathHelper.getExpectedResultsDir(), "vuln_entries.csv");

        Mockito.when(statistics.path.vulnEntries()).thenReturn(actual_vulnEntriesPath);
        Assertions.assertEquals(statistics.path.vulnEntries(), actual_vulnEntriesPath);

        statistics.saveVulnerabilityEntries(vulnEntries);

        Assertions.assertEquals(Util.readFile(expected_vulnEntriesPath), Util.readFile(actual_vulnEntriesPath));
    }

    @Test
    public void shouldCreateVulnStatistic() {
        final String actual_vulnEntriesResultPath = PathHandler.joinPath(PathHelper.getActualResultsDir(), "vuln_entries_result.csv");
        final String expected_vulnEntriesResultPath = PathHandler.joinPath(PathHelper.getExpectedResultsDir(), "vuln_entries_result.csv");

        Mockito.when(statistics.path.vulnEntryStatistics()).thenReturn(actual_vulnEntriesResultPath);
        Assertions.assertEquals(statistics.path.vulnEntryStatistics(), actual_vulnEntriesResultPath);

        statistics.createVulnStatistic(vulnEntries, PathHelper.getPatchesPath());

        List<List<String>> actual = Util.readCSVFirstNColumns((actual_vulnEntriesResultPath), 4);
        List<List<String>> expected = Util.readCSVFirstNColumns((expected_vulnEntriesResultPath), 4);

        Assertions.assertEquals(expected, actual);
    }

    @Test
    public void shouldCreateRepairedVulnSum() throws IOException {
        final String actual_vulnFoundResultPath = PathHandler.joinPath(PathHelper.getActualResultsDir(), "vuln_found_result.txt");
        final String expected_vulnFoundResultPath = PathHandler.joinPath(PathHelper.getExpectedResultsDir(), "vuln_found_result.txt");

        statistics.createRepairedVulnSum(vulnEntries, PathHelper.getPatchesPath(), actual_vulnFoundResultPath);

        Assertions.assertEquals(Util.readFile(expected_vulnFoundResultPath), Util.readFile(actual_vulnFoundResultPath));
    }


}
