package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.cli.MavenCli;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MavenPatchCompiler extends GenericPatchCompiler {
    private static final Logger logger = LogManager.getLogger(MavenPatchCompiler.class);

    @Override
    public List<Pair<File, Patch<String>>> applyAndCompile(File srcLocation, List<Pair<File, Patch<String>>> patches, boolean runTests) {
        List<Pair<File, Patch<String>>> filteredPatches = new ArrayList<>();
        MavenCli cli = new MavenCli();
        List<String> argList = new ArrayList<>();
        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());
        if (!runTests) {
            argList.add("-DskipTests=true");
        }
        argList.add("clean");
        argList.add("package");
        String [] args = new String[argList.size()];
        argList.toArray(args);
        for (Pair<File, Patch<String>> patch : patches) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final String utf8 = StandardCharsets.UTF_8.name();
            applyPatch(patch, srcLocation);
            try (PrintStream buffer = new PrintStream(baos, true, utf8)) {
                cli.doMain(args, srcLocation.getAbsolutePath(), buffer, buffer);
                if (baos.toString().contains("BUILD SUCCESS")) {
                    filteredPatches.add(patch);
                }
            } catch (UnsupportedEncodingException e) {
                logger.error(e.getMessage());
            }
            revertPatch(patch, srcLocation);
        }

        return filteredPatches;
    }
}
