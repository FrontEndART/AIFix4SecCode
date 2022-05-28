package eu.assuremoss.utils;

import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.factories.ToolFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class ColumnInfoParserTest {
    private static VulnerabilityDetector vulnDetector;
    private static final String mockedResultsPath = String.valueOf(Paths.get("src","test", "resources", "mocked-results"));

    @BeforeAll
    static void beforeAll() throws IOException {
        Configuration config = new Configuration("config.properties");
        vulnDetector = ToolFactory.createOsa(config.properties);
    }

    private static List<CodeModel> mockedCodeModels() {
        List<CodeModel> resList = new ArrayList<>();

        String asg = String.valueOf(Paths.get(mockedResultsPath, "asg.jfif"));
        String graphXML = String.valueOf(Paths.get(mockedResultsPath, "graph.xml"));
        String findBugsXML = String.valueOf(Paths.get(mockedResultsPath, "FindBugs.xml"));

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asg)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.OSA_GRAPH_XML, new File(graphXML)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.FINDBUGS_XML, new File(findBugsXML)));

        return resList;
    }

    @Test
    void Should_Attach_Column_Info_For_Vulnerability_Entries() {
        var expectedEntries = getExpectedEntries();
        var result = vulnDetector.getVulnerabilityLocations(new File(""), mockedCodeModels());

        Assertions.assertEquals(expectedEntries, result);
    }

    private List<VulnerabilityEntry> getExpectedEntries() {
        var result = new ArrayList<VulnerabilityEntry>();

        String vulnEntriesPath = String.valueOf(Paths.get(mockedResultsPath, "vulnEntries.ser").toAbsolutePath());

        try {
            FileInputStream fileIn = new FileInputStream(vulnEntriesPath);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            result = (ArrayList<VulnerabilityEntry>) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException i) {
            i.printStackTrace();
        } catch (ClassNotFoundException c) {
            System.out.println("VulnerabilityEntry class not found");
            c.printStackTrace();
        }

        return result;
    }
}