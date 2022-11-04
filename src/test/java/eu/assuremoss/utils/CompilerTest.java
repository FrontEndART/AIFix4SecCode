package eu.assuremoss.utils;

import eu.assuremoss.utils.tools.JANAnalyser;
import eu.assuremoss.utils.tools.SourceCompiler;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.zip.DataFormatException;
import static eu.assuremoss.utils.MLogger.MLOG;

public class CompilerTest {
    String testProjectRelativePath = "test-project";
    String testProjectAbsolutePath = PathHandler.joinPath(Utils.getWorkingDir(), testProjectRelativePath);

    static Configuration config;
    private static PathHandler path;
    private static SourceCompiler compiler;

    @BeforeAll
    static void setup() throws IOException {
        config = new Configuration("config-example.properties", "mapping-example.properties");
        path = new PathHandler(config.properties);
        Utils.initResourceFiles(config.properties, path);
        MLOG = new MLogger(config.properties, "log.txt", path);
        compiler = new SourceCompiler(config.properties.getProperty("config.results_path"));
    }

    @Test
    void compileTest() {
        compiler.compile (new File(testProjectAbsolutePath), "mavenCLI", true,false);
    }

    @Test
    void spotBugsAnalysisTest() {
        String sources = PathHandler.joinPath(Utils.getWorkingDir(), "");
        //compiler.setFbFileListPath(new File(Paths.get(config.properties.getProperty("config.results_path"), "proba.txt")));
        compiler.analyze(config.properties.getProperty("config.spotbugs_bin"), true);
    }

    @Test
    void janTest() {
        String asgDir = String.valueOf(Paths.get(config.properties.getProperty("config.results_path"), "asg"));
        Utils.createDirectory(asgDir);
        Assertions.assertTrue(new File(asgDir).exists());

        JANAnalyser jan = new JANAnalyser(config.properties, asgDir);

        String compilationUnit = PathHandler.joinPath(testProjectAbsolutePath, "src\\main\\java\\example\\ArrayDemo.java");
        jan.analyze(compilationUnit, "example.ArrayDemo");
    }
}
