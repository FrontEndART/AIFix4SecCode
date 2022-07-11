package eu.assuremoss.utils;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import helpers.PathHelper;
import helpers.Util;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class StatisticsTest {
    private Statistics statistics;
    private List<VulnerabilityEntry> vulnEntries;

    @BeforeAll
    static void init() throws IOException {
        Files.createDirectories(Path.of(PathHelper.getActualResultsDir()));
    }

    @BeforeEach
    void setup() throws IOException, ClassNotFoundException {
        statistics = new Statistics(PathHelper.getPathHandler());
        vulnEntries = Util.readVulnerabilitiesFromSER(PathHelper.getVulnEntriesPath());

        Util.cleanUpGeneratedTestFiles();
    }

    @AfterEach
    void tearDown() throws IOException {
        Util.cleanUpGeneratedTestFiles();
    }

    @Test
    public void shouldSaveFoundVulnerabilities() throws IOException {
        final String actual_vulnFoundPath = PathHandler.joinPath(PathHelper.getActualResultsDir(), "vuln_found.txt");
        final String expected_vulnFoundPath = PathHandler.joinPath(PathHelper.getExpectedResultsDir(), "vuln_found.txt");

        statistics.path = Mockito.mock(PathHandler.class);
        Mockito.when(statistics.path.vulnFound()).thenReturn(actual_vulnFoundPath);
        Assertions.assertEquals(actual_vulnFoundPath, statistics.path.vulnFound());

        statistics.saveFoundVulnerabilities(vulnEntries);

        Assertions.assertEquals(Util.readFile(actual_vulnFoundPath), Util.readFile(expected_vulnFoundPath));
    }

    @Test
    public void shouldSaveVulnerabilityEntries() throws IOException {
        final String actual_vulnEntriesPath = PathHandler.joinPath(PathHelper.getActualResultsDir(), "vuln_entries.csv");
        final String expected_vulnEntriesPath = PathHandler.joinPath(PathHelper.getExpectedResultsDir(), "vuln_entries.csv");

        statistics.path = Mockito.mock(PathHandler.class);
        Mockito.when(statistics.path.vulnEntries()).thenReturn(actual_vulnEntriesPath);
        Assertions.assertEquals(actual_vulnEntriesPath, statistics.path.vulnEntries());

        statistics.saveVulnerabilityEntries(vulnEntries);

        Assertions.assertEquals(Util.readFile(actual_vulnEntriesPath), Util.readFile(expected_vulnEntriesPath));
    }
}
