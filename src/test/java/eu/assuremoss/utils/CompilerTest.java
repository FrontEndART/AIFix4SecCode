package eu.assuremoss.utils;

import eu.assuremoss.utils.tools.JANAnalyser;
import eu.assuremoss.utils.tools.SourceCompiler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;

import static eu.assuremoss.utils.Configuration.*;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class CompilerTest {
    String testProjectRelativePath = "test-project";
    String testProjectAbsolutePath = PathHandler.joinPath(Utils.getWorkingDir(), testProjectRelativePath);
    private static MLogger MLOG;
    static Configuration config;
    private static PathHandler path;
    private static SourceCompiler compiler;

    @BeforeAll
    static void setup() throws IOException {
        config = new Configuration("config-example.properties", "mapping-example.properties");
        path = new PathHandler(config.properties.getProperty(RESULTS_PATH_KEY), config.properties.getProperty(VALIDATION_RESULTS_PATH_KEY));
        Utils.initResourceFiles(config.properties, path);
        MLOG = new MLogger("log.txt", path, Configuration.isTestingEnabled(config.properties));
        compiler = new SourceCompiler(config.properties, false);
    }

    @Test
    void compileAndAnalyseTest() {
        assertTrue(compiler.compile (new File(testProjectAbsolutePath), true,false));
        compiler.analyze();
        String spotbugsresult = String.valueOf(Paths.get(config.properties.getProperty("config.results_path"), SPOTBUGS_RESULTFILE));
        assertTrue(new File(spotbugsresult).exists());
    }

    @Test
    void janTest() {
        String asgDir = String.valueOf(Paths.get(config.properties.getProperty("config.results_path"), "asg"));
        Utils.createDirectory(asgDir);
        assertTrue(new File(asgDir).exists());

        JANAnalyser jan = new JANAnalyser(config.properties, asgDir);

        String compilationUnit = PathHandler.joinPath(testProjectAbsolutePath, "src\\main\\java\\example\\ArrayDemo.java");
        jan.analyze(compilationUnit, "example.ArrayDemo");
        assertTrue(new File(String.valueOf(Paths.get(asgDir, "example", "ArrayDemo.jsi"))).exists());
    }
}
