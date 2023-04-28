package eu.assuremoss.utils;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TestInfoExtractorTest {
    @BeforeEach
    void setup() {
        TestInfoExtractor.fileName = "";
        TestInfoExtractor.patch = 0;
    }

    @Test
    void shouldGetUnitTestRowCSV() {
        String line = "Tests run: 10\n" +
                "Failures: 20\n" +
                "Errors: 34\n" +
                "Skipped: 3";
        assertEquals("test,1,10,20,34,3\n", TestInfoExtractor.getUnitTestRowCSV("test.txt", line));
    }

    @Test
    void shouldGetValue100() {
        assertEquals("100", TestInfoExtractor.getValue("Hello 100", "Hello ([0-9]+)"));
    }

    @Test
    void shouldGetValue50() {
        assertEquals("50", TestInfoExtractor.getValue("This ifdgDFTGHLSDFLSDF 50 DRFGDDRTTTR", "This [A-Za-z]+ ([0-9]+) [A-Za-z]+"));
    }

    @Test
    void shouldGetPatchNameBuild() {
        assertEquals("build", TestInfoExtractor.getPatchName("log.txt"));
    }

    @Test
    void shouldGetPatchNameIncreasePatchAmount() {
        TestInfoExtractor.fileName = "test.txt";
        TestInfoExtractor.getPatchName("test.txt");
        assertEquals(1, TestInfoExtractor.patch);
    }

    @Test
    void shouldGetPatchNameIncreasePatchAmountMany() {
        TestInfoExtractor.fileName = "test.txt";
        for (int i = 0; i < 7; i++) {
            TestInfoExtractor.getPatchName("test.txt");
        }
        assertEquals(7, TestInfoExtractor.patch);
    }

    @Test
    void shouldGetUnitTestHeaderCSV() {
        assertEquals("Vuln_ID,Patch,Tests run,Failures,Errors,Skipped\n", TestInfoExtractor.getUnitTestHeaderCSV());
    }
}
