package eu.assuremoss.utils.tools;

import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.PathHandler;
import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.factories.PatchCompilerFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;


public class SourceCompiler {
    private static final Logger LOG = LogManager.getLogger(SourceCompiler.class);
    private final String workingDir;
    private String lastCompiled = "";

    private String fbFileListPath;
    public SourceCompiler(String workingDir) {
        this.workingDir = workingDir;
    }

    public String compile(File srcLocation, String projectBuildToolKey, boolean isTestingEnabled, boolean isValidation) {
        PatchCompiler patchCompiler = PatchCompilerFactory.getPatchCompiler(projectBuildToolKey);
        patchCompiler.compile(srcLocation, isTestingEnabled, true);

        fbFileListPath = String.valueOf(Paths.get(workingDir, "fb_file_list.txt"));
        try (FileWriter fw = new FileWriter(fbFileListPath)) {
            lastCompiled = patchCompiler.getBuildDirectoryName();
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), patchCompiler.getBuildDirectoryName())));
        } catch (IOException e) {
            LOG.error(e);
        }
        return fbFileListPath;
    }

    public void setFbFileListPath(String path) {
        fbFileListPath = path;
    }

    public void analyze(String spotbugs, boolean isValidation) {
        if (fbFileListPath == null) return;
        //System.out.println("Working dir: " + workingDir + " " + spotbugs);
        String xmlName = String.valueOf(Paths.get(workingDir, "spotbugs.xml"));
        String[] command = new String[] {
                new File(spotbugs).getAbsolutePath(),
                "-textui",
                "-analyzeFromFile=" + fbFileListPath,
                "-xml:withMessages",
                "-output=" + xmlName
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

    }

    public String getLastCompiled() {
        return lastCompiled;
    }
}
