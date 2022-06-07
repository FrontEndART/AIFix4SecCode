package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.ProcessRunner;

import java.io.File;
import java.nio.file.Path;
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

        if (copyDependencies) {
            argList.add("build");

            if (!runTests) {
                argList.add("-x");
                argList.add("test");
            }
        } else {
            argList.add("clean");
        }

        String[] args = new String[argList.size()];
        argList.toArray(args);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESSFUL");
    }
}
