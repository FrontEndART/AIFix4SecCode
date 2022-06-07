package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.ProcessRunner;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
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
        List<String> argList = new ArrayList<>();

        String rootProjectPath = srcLocation.getAbsolutePath();

        System.setProperty("maven.multiModuleProjectDirectory", rootProjectPath);

        argList.add("mvn");
        argList.add("-f");
        argList.add(rootProjectPath);

        // argList.add("-Dpmd.failOnViolation=false");

        argList.add("-DskipTests=true");

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

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESS");
    }

}
