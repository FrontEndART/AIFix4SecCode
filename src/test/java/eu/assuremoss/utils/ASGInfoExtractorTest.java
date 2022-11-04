package eu.assuremoss.utils;

import eu.assuremoss.VulnerabilityRepairDriver;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.utils.parsers.ASGInfoParser;
import eu.assuremoss.utils.parsers.SpotBugsParser;
import org.eclipse.core.runtime.Path;
import org.junit.Assert;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.zip.DataFormatException;

public class ASGInfoExtractorTest {
    public static final String mockedResultsPath = String.valueOf(Paths.get("src", "test", "resources", "mocked-results"));

    //private static ASGInfoParser asgParser;
    private static VulnerabilityRepairDriver vulnerabilityDriver;
    static Configuration config;
    private static SpotBugsParser parser;
    private static List<CodeModel> mocked_models;

    private static List<CodeModel> mockedCodeModels() {
        List<CodeModel> resList = new ArrayList<>();

        String asg = String.valueOf(Paths.get(mockedResultsPath, "test-project.ljsi"));
        String spotBugsXML = String.valueOf(Paths.get(mockedResultsPath, "spotBugs.xml"));

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asg)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.SPOTBUGS_XML, new File(spotBugsXML)));

        return resList;
    }

    @BeforeAll
    static void beforeAll() throws IOException, DataFormatException {
        mocked_models = mockedCodeModels();
        //asgParser = new ASGInfoParser(Utils.getCodeModel(mocked_models, CodeModel.MODEL_TYPES.ASG).get().getModelPath());
        config = new Configuration("config-example.properties", "mapping-example.properties");
        parser = new SpotBugsParser(mocked_models, config, false);
    }

    @Test
    void checkAnalyzedClasses() {
        /*Map<String, Integer> result =asgParser.getAnalyzedClasses().entrySet().stream().filter(map->map.getKey().startsWith("example")).collect(Collectors.toMap(map -> map.getKey(), map -> map.getValue()));
        Assert.assertEquals("The number of example classes", 4, result.size());*/

    }

    @Test
    void checkSpotBugsParser() throws DataFormatException {
        parser.readXML();
    }
}
