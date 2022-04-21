package eu.assuremoss.utils;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

public class UtilsTest {

    private static Properties properties = new Properties();

    @BeforeClass
    public static void initProperties() {
        properties.setProperty("mapping.FB_EiER", "EI_EXPOSE_REP2");
        properties.setProperty("mapping.FB_EER", "EI_EXPOSE_REP2");
    }

    @Test
    public void testWarningMapping()
    {
        var mapping = Utils.getWarningMappingFromProp(properties);
        assertEquals(mapping.getOrDefault("FB_EiER",""), "EI_EXPOSE_REP2");
    }

    @Test
    public void testWarningMappingSame()
    {
        var mapping = Utils.getWarningMappingFromProp(properties);
        assertEquals(mapping.getOrDefault("FB_EiER",""), mapping.getOrDefault("FB_EER",""));
    }
}
