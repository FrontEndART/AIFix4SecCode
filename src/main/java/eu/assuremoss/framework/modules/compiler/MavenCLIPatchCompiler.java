package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.Utils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.cli.CliRequest;
import org.apache.maven.cli.MavenCli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MavenCLIPatchCompiler extends GenericPatchCompiler {
    private static final Logger LOG = LogManager.getLogger(MavenCLIPatchCompiler.class);
    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = String.valueOf(Paths.get("target", "classes"));
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());

        List<String> argsList = getMavenArgs(srcLocation, runTests, copyDependencies);
        MavenCli cli = new MavenCli();

        String[] args = new String[argsList.size()];
        argsList.toArray(args);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final ByteArrayOutputStream baos_err = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();

        int compilationResult = 1;
        try (PrintStream buffer = new PrintStream(baos, true, utf8);PrintStream buffer_err = new PrintStream(baos_err, true, utf8)) {
            compilationResult = cli.doMain(args, srcLocation.getAbsolutePath(), buffer, buffer_err);

            if (compilationResult != 0) {
                MLogger.getActiveLogger().info("ERROR - BUILD FAILED! - " + srcLocation.getName());
                MLogger.getActiveLogger().fInfo(baos_err.toString());
            }

        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }

        return compilationResult == 0;
    }

    private List<String> getMavenArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        return mavenDefaultArgs(srcLocation, runTests, copyDependencies);
    }

    public List<String> mavenDefaultArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> args = new ArrayList<>();

        args.add("-f");
        args.add(srcLocation.getAbsolutePath());

        args.add("-Dpmd.failOnViolation=false");
        if (!runTests) args.add("-DskipTests=true");

        args.add("clean");
        args.add("package");

        return args;
    }
}
