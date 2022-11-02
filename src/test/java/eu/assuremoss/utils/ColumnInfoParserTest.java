package eu.assuremoss.utils;

import eu.assuremoss.framework.api.VulnerabilityDetector;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import helpers.VulnEntryHelper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class ColumnInfoParserTest {
    /*private static VulnerabilityDetector vulnDetector;
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

        Assertions.assertEquals(new Pair<>(34, 51), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForEiER2() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main",  "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable("permissionsToNeeded");
        vulnEntry.setStartLine(29);
        vulnEntry.setEndLine(29);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(36, 55), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForEiER3() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable("permissionsToNotNeeded");
        vulnEntry.setStartLine(34);
        vulnEntry.setEndLine(34);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(39, 61), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForEiER4() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.ArrayDemo.withPermissionsToGive(String[]) may expose internal representation by storing an externally mutable object into ArrayDemo.permissionsToGive");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "ArrayDemo.java")));
        vulnEntry.setVariable("actions");
        vulnEntry.setStartLine(40);
        vulnEntry.setEndLine(40);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(24, 31), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForMSBF() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("MS_SHOULD_BE_FINAL");
        vulnEntry.setVulnType("FB_MSBF");
        vulnEntry.setDescription("example.Main.MY_CONSTANT isn't final but should be");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "Main.java")));
        vulnEntry.setVariable("example.main.MY_CONSTANT");
        vulnEntry.setStartLine(3);
        vulnEntry.setEndLine(3);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(26, 37), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForEER() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP");
        vulnEntry.setVulnType("FB_EER");
        vulnEntry.setDescription("example.MyDate.getDate() may expose internal representation by returning MyDate.date");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "MyDate.java")));
        vulnEntry.setVariable(null);
        vulnEntry.setStartLine(12);
        vulnEntry.setEndLine(12);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(16, 20), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForEiER5() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("EI_EXPOSE_REP2");
        vulnEntry.setVulnType("FB_EiER");
        vulnEntry.setDescription("example.MyDate.setDate(Date) may expose internal representation by storing an externally mutable object into MyDate.date");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "MyDate.java")));
        vulnEntry.setVariable("date");
        vulnEntry.setStartLine(16);
        vulnEntry.setEndLine(16);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(21, 25), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForNNOSP() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("NP_NULL_ON_SOME_PATH");
        vulnEntry.setVulnType("FB_NNOSP");
        vulnEntry.setDescription("Possible null pointer dereference of null in example.NullPath.foo(String)");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "NullPath.java")));
        vulnEntry.setVariable("str");
        vulnEntry.setStartLine(7);
        vulnEntry.setEndLine(7);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(20, 23), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForFSBP() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("FI_PUBLIC_SHOULD_BE_PROTECTED");
        vulnEntry.setVulnType("FB_FSBP");
        vulnEntry.setDescription("Finalize method should be protected");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "FinalizeExample.java")));
        vulnEntry.setVariable("finalize");
        vulnEntry.setStartLine(16);
        vulnEntry.setEndLine(17);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(5, 6), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForMMC() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("MS_MUTABLE_COLLECTION");
        vulnEntry.setVulnType("FB_MMC");
        vulnEntry.setDescription("Finalize method should be protected");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "a/MutableInit.java")));
        vulnEntry.setVariable("mySet2");
        vulnEntry.setStartLine(8);
        vulnEntry.setEndLine(8);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(37, 43), columnInfo);
    }

    @Test
    void shouldGetColumnInfoForMP() {
        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();
        vulnEntry.setType("MS_PKGPROTECT");
        vulnEntry.setVulnType("FB_MP");
        vulnEntry.setDescription("Field should be package protected");
        vulnEntry.setPath(String.valueOf(Paths.get("test-project", "src", "main", "java", "example", "a/Mutable.java")));
        vulnEntry.setVariable("mySet2");
        vulnEntry.setStartLine(8);
        vulnEntry.setEndLine(8);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        Assertions.assertEquals(new Pair<>(37, 43), columnInfo);
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
    }*/

    /*@Disabled
    @Test
    void Should_Attach_Column_Info_For_Vulnerability_Entries() {
        var expectedEntries = VulnEntryHelper.getVulnEntries();
        var result = vulnDetector.getVulnerabilityLocations(mockedCodeModels());

        Assertions.assertEquals(expectedEntries, result);
    }*/
}
