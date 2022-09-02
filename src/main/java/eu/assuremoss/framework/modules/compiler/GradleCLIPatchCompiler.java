package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.ProcessRunner;

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class GradleCLIPatchCompiler extends GenericPatchCompiler {

    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = String.valueOf(Paths.get("build", "classes"));
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> argList = new ArrayList<>();

        argList.add("gradle");
        argList.add("tasks");
        argList.add("-p");
        argList.add(srcLocation.getAbsolutePath());

        if (!runTests) {
            argList.add("-x");
            argList.add("test");
        }

        argList.add("clean");
        argList.add("build");

        ProcessBuilder processBuilder = new ProcessBuilder(argList);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESSFUL");
    }
}
