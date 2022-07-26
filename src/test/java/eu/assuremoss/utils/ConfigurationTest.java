package eu.assuremoss.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static eu.assuremoss.utils.Configuration.ARCHIVE_ENABLED;
import static org.junit.jupiter.api.Assertions.*;

public class ConfigurationTest {
    static Configuration config;

    @BeforeAll
    static void setup() throws IOException {
        config = new Configuration("config-example.properties", "mapping-example.properties");
    }

    @Test
    void shouldReadGeneralSettings() {
        assertEquals("test-project", config.properties.getProperty("config.project_name"));
        assertEquals(PathHandler.joinPath(Utils.getWorkingDir(), "test-project"), config.properties.getProperty("config.project_path"));
        assertEquals("src\\main\\java", config.properties.getProperty("config.project_source_path"));
        assertEquals("maven", config.properties.getProperty("config.project_build_tool"));
        assertEquals("d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java", config.properties.getProperty("config.osa_path"));
        assertEquals("OpenStaticAnalyzer", config.properties.getProperty("config.osa_edition"));
        assertEquals("d:\\AIFix4SecCode\\test-project\\results", config.properties.getProperty("config.results_path"));
        assertEquals("d:\\AIFix4SecCode\\test-project\\validation", config.properties.getProperty("config.validation_results_path"));
        assertEquals("true", config.properties.getProperty("config.archive_enabled"));
        assertEquals("d:\\AIFix4SecCode\\test-project\\archive", config.properties.getProperty("config.archive_path"));
    }

    @Test
    void shouldReadMapping() {
        assertEquals("EI_EXPOSE_REP2", config.properties.getProperty("mapping.FB_EiER"));
        assertEquals("EI_EXPOSE_REP", config.properties.getProperty("mapping.FB_EER"));
        assertEquals("MS_SHOULD_BE_FINAL", config.properties.getProperty("mapping.FB_MSBF"));
        assertEquals("NP_NULL_ON_SOME_PATH", config.properties.getProperty("mapping.FB_NNOSP"));
        assertEquals("NP_NULL_ON_SOME_PATH_EXCEPTION", config.properties.getProperty("mapping.FB_NNOSPE"));
    }

    @Test
    void shouldReadStrategies() {
        assertEquals("EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2", config.properties.getProperty("strategy.EI_EXPOSE_REP2"));
        assertEquals("EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2", config.properties.getProperty("strategy.EI_EXPOSE_REP"));
        assertEquals("MS_SHOULD_BE_FINAL", config.properties.getProperty("strategy.MS_SHOULD_BE_FINAL"));
        assertEquals("NP_NULL_ON_SOME_PATH", config.properties.getProperty("strategy.NP_NULL_ON_SOME_PATH"));
        assertEquals("NP_NULL_ON_SOME_PATH_EXCEPTION", config.properties.getProperty("strategy.NP_NULL_ON_SOME_PATH_EXCEPTION"));
    }

    @Test
    void shouldReadDescriptions() {
        assertEquals("Repair with Arrays.copyOf", config.properties.getProperty("desc.EI_EXPOSE_REP2_ARRAY"));
        assertEquals("Repair with creating new Date", config.properties.getProperty("desc.EI_EXPOSE_REP2_DATEOBJECT"));
        assertEquals("Repair with clone", config.properties.getProperty("desc.EI_EXPOSE_REP2"));
        assertEquals("Repair with adding final", config.properties.getProperty("desc.MS_SHOULD_BE_FINAL"));
        assertEquals("Repair with null-check in ternary", config.properties.getProperty("desc.NP_NULL_ON_SOME_PATH"));
        assertEquals("Repair with null-check in ternary", config.properties.getProperty("desc.NP_NULL_ON_SOME_PATH_EXCEPTION"));
    }

    @Test
    void shouldArchiveBeEnabled() {
        config.properties.setProperty(ARCHIVE_ENABLED, "true");
        assertTrue(Configuration.archiveEnabled(config.properties));
    }

    @Test
    void shouldArchiveBeDisabled() {
        config.properties.setProperty(ARCHIVE_ENABLED, "false");
        assertFalse(Configuration.archiveEnabled(config.properties));
    }

    @Test
    void shouldGetDescriptionPath() {
        assertEquals(PathHandler.joinPath("d:\\AIFix4SecCode\\test-project\\results", "osa_xml"), Configuration.descriptionPath(config.properties));
    }

    @Test
    void shouldGetPatchSavePath() {
        assertEquals(PathHandler.joinPath("d:\\AIFix4SecCode\\test-project\\results", "patches"), Configuration.patchSavePath(config.properties));
    }
}
