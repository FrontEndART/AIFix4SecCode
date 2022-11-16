package eu.assuremoss.utils.tools;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.PathHandler;
import eu.assuremoss.utils.ProcessRunner;
import eu.assuremoss.utils.factories.PatchCompilerFactory;
import eu.assuremoss.utils.parsers.SpotBugsParser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.*;


public class SourceCompiler {
    private static final Logger LOG = LogManager.getLogger(SourceCompiler.class);
    private final String workingDir;
    private String lastCompiled = "";
    private PatchCompiler patchCompiler;
    private String spotbugs;

    private String analyzedClasses;
    public SourceCompiler(Properties properties, boolean isValidation) {
        patchCompiler = PatchCompilerFactory.getPatchCompiler(properties.getProperty(PROJECT_BUILD_TOOL_KEY));
        this.workingDir = !isValidation?String.valueOf(Paths.get(properties.getProperty(RESULTS_PATH_KEY)))
                                        :String.valueOf(Paths.get(properties.getProperty(VALIDATION_RESULTS_PATH_KEY)));
        spotbugs = properties.getProperty(SPOTBUGS_BIN);
    }

    public boolean compile(File srcLocation, boolean isTestingEnabled, boolean isValidation) {

        boolean isCompilable = patchCompiler.compile(srcLocation, isTestingEnabled, true);

        analyzedClasses = String.valueOf(Paths.get(workingDir, SPOTBUGS_LISTFILE));
        try (FileWriter fw = new FileWriter(analyzedClasses)) {
            lastCompiled = patchCompiler.getBuildDirectoryName();
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(), patchCompiler.getBuildDirectoryName())));
        } catch (IOException e) {
            LOG.error(e);
        }
        return isCompilable;
    }

    public void setContentOfAnalyzedClasses(File srcLocation, String classname) {
        analyzedClasses = String.valueOf(Paths.get(workingDir, SPOTBUGS_LISTFILE));
        try (FileWriter fw = new FileWriter(analyzedClasses)) {
            fw.write(String.valueOf(Paths.get(srcLocation.getAbsolutePath(),patchCompiler.getBuildDirectoryName(), classname.replaceAll("\\.", Matcher.quoteReplacement(File.separator))+".class")));
        } catch (IOException e) {
            LOG.error(e);
        }

    }

    public void analyze(boolean isValidation) {
        if (analyzedClasses == null) return;
        //System.out.println("Working dir: " + workingDir + " " + spotbugs);
        String xmlName = String.valueOf(Paths.get(workingDir, "spotbugs.xml"));
        String[] command = new String[] {
                new File(spotbugs).getAbsolutePath(),
                "-textui",
                "-analyzeFromFile=" + analyzedClasses,
                "-xml:withMessages",
                "-output=" + xmlName
        };
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        ProcessRunner.run(processBuilder);

    }

    public String getLastCompiled() {
        return lastCompiled;
    }

    public boolean checkPatch(PathHandler path, Properties properties, SpotBugsParser originalParser, File sourceLocation, VulnerabilityEntry vulnEntry, Pair<File, Patch<String>> patch, boolean isTestingEnabled) {
        boolean isCandidate = true;
        patchCompiler.applyPatch(patch, sourceLocation);
        isCandidate = compile (sourceLocation,  isTestingEnabled, true);
        if (isCandidate) {
            vulnEntry.setFilteredPatches(vulnEntry.getFilteredPatches()+1);
            setContentOfAnalyzedClasses(sourceLocation, vulnEntry.getClassName());
            analyze(true);
            SpotBugsParser patchParser = new SpotBugsParser(path, properties, null );
            List<VulnerabilityEntry> newVulnerabilities = null;
            try {
                newVulnerabilities = patchParser.readXML(true, false);
            } catch (DataFormatException e) {
                e.printStackTrace();
            }
            isCandidate = originalParser.checkQualityImprovements(vulnEntry.getClassName(), vulnEntry, newVulnerabilities);
        }

        patchCompiler.revertPatch(patch, sourceLocation);
        return isCandidate;
    }
}
