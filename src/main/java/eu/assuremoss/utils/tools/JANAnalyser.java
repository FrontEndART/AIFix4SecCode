package eu.assuremoss.utils.tools;

import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.Utils;

import java.io.File;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;

import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;
import static eu.assuremoss.utils.Configuration.PROJECT_SOURCE_PATH_KEY;

public class JANAnalyser {

    String binDir;
    String janEdition;
    String janCompiler;
    String j2cpPath;
    String j2cpEdition;
    String asgDir;
    String projectSrc;
    File srcLocation;
    String jsiName;

    public JANAnalyser(Properties properties, String asgDir) {
        binDir = properties.getProperty("config.jan_path");
        this.asgDir = asgDir;
        janEdition = properties.getProperty("config.jan_edition");
        janCompiler = properties.getProperty("config.jan_compiler");
        srcLocation = new File(properties.getProperty(PROJECT_PATH_KEY));
        j2cpPath = properties.getProperty("config.additional_tools_path");
        j2cpEdition = properties.getProperty("config.jan2changepath_edition");
        projectSrc = properties.getProperty(PROJECT_SOURCE_PATH_KEY);
        if (asgDir != null)
            Utils.createDirectory(asgDir);
    }

    public String analyze(String compilationUnitPath, String compilationUnitName) {
        String cuName = compilationUnitName.replaceAll("\\.", Matcher.quoteReplacement(File.separator));
        jsiName = String.valueOf(Paths.get(asgDir, cuName+".jsi"));
        String xmlName = String.valueOf(Paths.get(asgDir, cuName+".xml"));


        String[] command = new String[] {
                "java",
                "--patch-module",
                "jdk.compiler=" + String.valueOf(Paths.get(binDir, janCompiler)),
                "-jar",
                String.valueOf(Paths.get(binDir, janEdition)),
                "-i",
                compilationUnitPath,
                "-o",
                jsiName,
                "-x",
                xmlName
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

        command = new String[] {
                new File(j2cpPath, j2cpEdition + Utils.getExtension()).getAbsolutePath(),
                jsiName,
                "-from:" + Paths.get(srcLocation.getAbsolutePath(), projectSrc) + File.separator,
                "-to:"
        };
        processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

        return jsiName;
    }

}
