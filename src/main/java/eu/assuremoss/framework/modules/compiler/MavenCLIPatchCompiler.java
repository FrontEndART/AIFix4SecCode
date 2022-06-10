package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.ProcessRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MavenCLIPatchCompiler extends GenericPatchCompiler {

    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = String.valueOf(Paths.get("target", "classes"));
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> argList = new ArrayList<>();

        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());
        argList.add("mvn");
        argList.add("-f");
        argList.add(srcLocation.getAbsolutePath());

        argList.add("-Dpmd.failOnViolation=false");
        if (!runTests) argList.add("-DskipTests=true");

        argList.add("clean");
        argList.add("package");

        ProcessBuilder processBuilder = new ProcessBuilder(argList);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESS");
    }

}
