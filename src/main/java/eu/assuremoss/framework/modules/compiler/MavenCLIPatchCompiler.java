package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.Utils;

import java.io.File;
import java.nio.file.Paths;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

public class MavenCLIPatchCompiler extends GenericPatchCompiler {

    @Override
    protected void initBuildDirectoryName() {
        buildDirectoryName = String.valueOf(Paths.get("target", "classes"));
    }

    @Override
    public boolean compile(File srcLocation, boolean runTests, boolean copyDependencies) {
        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());

        List<String> args = getMavenArgs(srcLocation, runTests, copyDependencies);

        ProcessBuilder processBuilder = new ProcessBuilder(args);
        String message = ProcessRunner.runAndReturnMessage(processBuilder);

        return message.contains("BUILD SUCCESS");
    }

    private List<String> getMavenArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        switch (Utils.getOsName()) {
            case "Windows":
                return windowsArgs(srcLocation, runTests, copyDependencies);
            case "Linux":
                return linuxArgs(srcLocation, runTests, copyDependencies);
            default:
                throw new InvalidParameterException("Unsupported OS type: " + Utils.getOsName());
        }
    }

    public List<String> mavenDefaultArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> args = new ArrayList<>();

        args.add("mvn");
        args.add("-f");
        args.add(srcLocation.getAbsolutePath());

        args.add("-Dpmd.failOnViolation=false");
        if (!runTests) args.add("-DskipTests=true");

        args.add("clean");
        args.add("package");

        return args;
    }

    private List<String> linuxArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        return mavenDefaultArgs(srcLocation, runTests, copyDependencies);
    }

    public List<String> windowsArgs(File srcLocation, boolean runTests, boolean copyDependencies) {
        List<String> argList = mavenDefaultArgs(srcLocation, runTests, copyDependencies);

        argList.add(0, "cmd");
        argList.add(1, "/c");

        return argList;
    }

}
