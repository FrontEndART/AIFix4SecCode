package eu.assuremoss.utils;

import eu.assuremoss.VulnerabilityRepairDriver;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.parsers.ASGInfoParser;
import eu.assuremoss.utils.parsers.SpotBugsParser;
import helpers.PathHelper;
import helpers.Util;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;
import static eu.assuremoss.utils.Configuration.VALIDATION_RESULTS_PATH_KEY;


public class ASGInfoExtractorTest {
    private static final String mockedResultsPath = String.valueOf(Paths.get("src", "test", "resources", "mocked-results"));

    private static ASGInfoParser asgParser;
    private static VulnerabilityRepairDriver vulnerabilityDriver;
    private static Configuration config;
    private static final String asg = String.valueOf(Paths.get(mockedResultsPath, "test-project.ljsi"));
    private static final  String spotBugsXML = String.valueOf(Paths.get(mockedResultsPath, "spotBugs.xml"));
    private static SpotBugsParser parser;
    private static PathHandler path;
    private static MLogger MLOG;


    @BeforeAll
    static void beforeAll() throws IOException, DataFormatException {
        config = new Configuration("config-example.properties", "mapping-example.properties");
        path = new PathHandler(config.properties.getProperty(RESULTS_PATH_KEY), config.properties.getProperty(VALIDATION_RESULTS_PATH_KEY));
        MLOG = new MLogger("log.txt", path, Configuration.isTestingEnabled(config.properties));
        asgParser = new ASGInfoParser(new File(asg));
        parser = new SpotBugsParser(path, config.properties);
        Utils.initResourceFiles(config.properties, path);
    }

    @Test
    void checkAnalyzedClasses() {
        Map<String, Integer> result =asgParser.getAnalyzedClasses().entrySet().stream().filter(map->map.getKey().startsWith("example")).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
        Assert.assertEquals("The number of example classes", 8, result.size());
    }

    @Test
    void checkSpotBugsParser() throws DataFormatException, IOException {
       List<VulnerabilityEntry> listOfVulnerabilities = parser.readXML(false, true);
       Util.writeVulnerabilitiesToSER(listOfVulnerabilities, PathHelper.getVulnEntriesPath());
    }
}
