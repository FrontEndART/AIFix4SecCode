package eu.assuremoss.framework.modules.compiler;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.maven.cli.MavenCli;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static eu.assuremoss.VulnRepairDriver.MLOG;

public class MavenPatchCompiler extends GenericPatchCompiler {
    private static final Logger LOG = LogManager.getLogger(MavenPatchCompiler.class);

    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = String.valueOf(Paths.get("target", "classes"));
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        MavenCli cli = new MavenCli();

        List<String> argList = new ArrayList<>();
        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());
        if (!runTests) {
            argList.add("-DskipTests=true");
        }
        if (copyDependencies) {
            try {
                URL inputUrl = Thread.currentThread().getContextClassLoader().getResource("dep-pom.xml");
                File dest = new File(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), "dep-pom.xml")));
                FileUtils.copyURLToFile(inputUrl, dest);
                argList.add("-f");
                argList.add("dep-pom.xml");
                argList.add("package");
                argList.add("dependency:copy-dependencies");
            } catch (IOException e) {
                LOG.error(e);
                return false;
            }
        } else {
            argList.add("clean");
            argList.add("package");
        }
        String[] args = new String[argList.size()];
        argList.toArray(args);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        try (PrintStream buffer = new PrintStream(baos, true, utf8)) {
            cli.doMain(args, srcLocation.getAbsolutePath(), buffer, buffer);
            String mvnOutput = baos.toString();
            MLOG.fInfo(mvnOutput);
            if (mvnOutput.contains("BUILD SUCCESS")) {
                return true;
            }
            baos.close();
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
        MLOG.info("ERROR - BUILD FAILED!");
        return false;
    }
}
