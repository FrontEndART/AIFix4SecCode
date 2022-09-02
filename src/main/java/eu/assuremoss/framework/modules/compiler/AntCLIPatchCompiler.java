package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.ProcessRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AntCLIPatchCompiler extends GenericPatchCompiler{

    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = "bin";
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> args = new ArrayList<>();

        args.add("ant");
        args.add("-f");
        args.add(srcLocation.getAbsolutePath());

        args.add("clean");
        args.add("build");

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESSFUL");
    }
}
