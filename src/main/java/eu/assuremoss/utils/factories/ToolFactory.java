package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.framework.modules.repair.ASGTransformRepair;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.Utils;

import java.nio.file.Paths;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.*;

public class ToolFactory {
    public static OpenStaticAnalyzer createOsa(Properties properties) {
        return new OpenStaticAnalyzer(
                properties.getProperty(OSA_PATH_KEY),
                properties.getProperty(OSA_EDITION_KEY),
                String.valueOf(Paths.get(properties.getProperty(OSA_PATH_KEY), (System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("windows") ? "WindowsTools" : "LinuxTools"))),
                "JAN2ChangePath",
                properties.getProperty(RESULTS_PATH_KEY),
                properties.getProperty(VALIDATION_RESULTS_PATH_KEY),
                properties.getProperty(PROJECT_NAME_KEY),
                Utils.getWarningMappingFromProp(properties));
    }

    public static ASGTransformRepair createASGTransformRepair(Properties properties) {
        return new ASGTransformRepair(
                properties.getProperty(PROJECT_PATH_KEY),
                properties.getProperty(RESULTS_PATH_KEY),
                String.valueOf(Paths.get(properties.getProperty(RESULTS_PATH_KEY), "osa_xml")),
                String.valueOf(Paths.get(properties.getProperty(RESULTS_PATH_KEY), "patches")),
                Utils.getFixStrategies(properties));
    }
}
