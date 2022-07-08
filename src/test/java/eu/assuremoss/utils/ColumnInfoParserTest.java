package eu.assuremoss.utils;

import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
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
        Configuration config = new Configuration("config.properties", "mapping.properties");
        //  vulnDetector = ToolFactory.createOsa(config.properties);
    }

    @Test
    void shouldGetColumnInfoForEiER1() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(24);
        vulnEntry.setEndLine(24);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(34, 51));
    }

    @Test
    void shouldGetColumnInfoForEiER2() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main",  "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(29);
        vulnEntry.setEndLine(29);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(36, 55));
    }

    @Test
    void shouldGetColumnInfoForEiER3() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(34);
        vulnEntry.setEndLine(34);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(39, 61));
    }

    @Test
    void shouldGetColumnInfoForEiER4() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(40);
        vulnEntry.setEndLine(40);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(24, 31));
    }

    @Test
    void shouldGetColumnInfoForMSBF() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("MS_SHOULD_BE_FINAL");
        vulnEntry.setVulnType("FB_MSBF");
        vulnEntry.setDescription("example.Main.MY_CONSTANT isn't final but should be");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "Main.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(3);
        vulnEntry.setEndLine(3);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(-1, -1));
    }

    @Test
    void shouldGetColumnInfoForEER() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP");
        vulnEntry.setVulnType("FB_EER");
        vulnEntry.setDescription("example.MyDate.getDate() may expose internal representation by returning MyDate.date");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "MyDate.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(3);
        vulnEntry.setEndLine(3);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(-1, -1));
    }

    @Test
    void shouldGetColumnInfoForEiER5() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.MyDate.setDate(Date) may expose internal representation by storing an externally mutable object into MyDate.date");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "MyDate.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(16);
        vulnEntry.setEndLine(16);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(21, 25));
    }

    @Test
    void shouldGetColumnInfoForNNOSP() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("NP_NULL_ON_SOME_PATH");
        vulnEntry.setVulnType("FB_NNOSP");
        vulnEntry.setDescription("Possible null pointer dereference of null in example.NullPath.foo(String)");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "NullPath.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(7);
        vulnEntry.setEndLine(7);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(columnInfo, new Pair<>(20, 23));
    }

    private static List<CodeModel> mockedCodeModels() {
        List<CodeModel> resList = new ArrayList<>();

        // TODO should remove absolute path from graph.xml
        String asg = String.valueOf(Paths.get(mockedResultsPath, "asg.jfif"));
        String graphXML = String.valueOf(Paths.get(mockedResultsPath, "graph.xml"));
        String findBugsXML = String.valueOf(Paths.get(mockedResultsPath, "FindBugs.xml"));

        resList.add(new CodeModel(CodeModel.MODEL_TYPES.ASG, new File(asg)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.OSA_GRAPH_XML, new File(graphXML)));
        resList.add(new CodeModel(CodeModel.MODEL_TYPES.FINDBUGS_XML, new File(findBugsXML)));

        return resList;
    }

    @Disabled
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
