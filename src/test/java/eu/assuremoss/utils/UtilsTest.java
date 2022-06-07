package eu.assuremoss.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

    private static Properties properties = new Properties();

    @BeforeAll
    static void initProperties() {
        properties.setProperty("mapping.FB_EiER", "EI_EXPOSE_REP2");
        properties.setProperty("mapping.FB_EER", "EI_EXPOSE_REP2");
        properties.setProperty("strategy.EI_EXPOSE_REP2", "EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2");
        properties.setProperty("strategy.MS_SHOULD_BE_FINAL", "MS_SHOULD_BE_FINAL");
        properties.setProperty("desc.EI_EXPOSE_REP2_ARRAY", "Repair with Arrays.copyOf");
        properties.setProperty("desc.EI_EXPOSE_REP2_DATEOBJECT", "Repair with creating new Date");
        properties.setProperty("desc.EI_EXPOSE_REP2", "Repair with clone");
        properties.setProperty("desc.MS_SHOULD_BE_FINAL", "Repair with adding final");
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
