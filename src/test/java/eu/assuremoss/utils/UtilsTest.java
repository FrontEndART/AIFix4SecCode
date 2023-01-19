package eu.assuremoss.utils;

import helpers.PathHelper;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.RESULTS_PATH_KEY;
import static eu.assuremoss.utils.Configuration.VALIDATION_RESULTS_PATH_KEY;
import static org.junit.Assert.assertEquals;

public class UtilsTest {

    private static Properties properties = new Properties();

    public static String getIntermediatePatchesDir() {
        return PathHandler.joinPath(PathHelper.testResultsPath, "intermediatePatches");
    }

    @BeforeAll
    static void initProperties() throws IOException {
        properties.setProperty("mapping.FB_EiER", "EI_EXPOSE_REP2");
        properties.setProperty("mapping.FB_EER", "EI_EXPOSE_REP2");
        properties.setProperty("strategy.EI_EXPOSE_REP2", "EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2");
        properties.setProperty("strategy.MS_SHOULD_BE_FINAL", "MS_SHOULD_BE_FINAL");
        properties.setProperty("desc.EI_EXPOSE_REP2_ARRAY", "Repair with Arrays.copyOf");
        properties.setProperty("desc.EI_EXPOSE_REP2_DATEOBJECT", "Repair with creating new Date");
        properties.setProperty("desc.EI_EXPOSE_REP2", "Repair with clone");
        properties.setProperty("desc.MS_SHOULD_BE_FINAL", "Repair with adding final");
        properties.setProperty("config.results_path", PathHelper.testResultsPath);

        Files.createDirectories(Path.of(PathHandler.joinPath(PathHelper.testResultsPath, "logs")));
    }

    @Test
    public void shouldDeleteIntermediatePatches() throws IOException {
        // Setup dir with patches
        Files.createDirectories(Path.of(getIntermediatePatchesDir()));
        FileUtils.cleanDirectory(new File(getIntermediatePatchesDir()));

        Utils.createEmptyLogFile(properties);
        MLogger MLOG = new MLogger("log.txt", new PathHandler(properties.getProperty(RESULTS_PATH_KEY), properties.getProperty(VALIDATION_RESULTS_PATH_KEY)), Configuration.isTestingEnabled(properties));

        for (int i = 1; i <= 5; i++) {
            String fileName = String.format("repair_patch%d.diff", i);
            Files.createFile(Path.of(PathHandler.joinPath(getIntermediatePatchesDir(), fileName)));
        }

        Utils.deleteIntermediatePatches(getIntermediatePatchesDir());

        Assertions.assertEquals(0, new File(getIntermediatePatchesDir()).list().length);
    }

    @Test
    public void testWarningMapping() {
        var mapping = Utils.getWarningMappingFromProp(properties);
        assertEquals("EI_EXPOSE_REP2", mapping.getOrDefault("FB_EiER", ""));
    }

    @Test
    public void testWarningMappingSame() {
        var mapping = Utils.getWarningMappingFromProp(properties);
        assertEquals(mapping.getOrDefault("FB_EiER", ""), mapping.getOrDefault("FB_EER", ""));
    }

    @Test
    public void testFixStrategiesSingle() {
        var fixStrategies = Utils.getFixStrategies(properties);
        assertEquals("Repair with adding final", fixStrategies.get("MS_SHOULD_BE_FINAL").get("MS_SHOULD_BE_FINAL"));
    }

    @Test
    public void testFixStrategiesMulti() {
        var fixStrategies = Utils.getFixStrategies(properties);
        assertEquals("Repair with Arrays.copyOf", fixStrategies.get("EI_EXPOSE_REP2").get("EI_EXPOSE_REP2_ARRAY"));
    }
}
