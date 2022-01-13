package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import eu.assuremoss.utils.Pair;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.cli.MavenCli;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class MavenPatchCompiler extends GenericPatchCompiler {
    private static final Logger LOG = LogManager.getLogger(MavenPatchCompiler.class);

    @Override
    public boolean compile(File srcLocation, boolean runTests) {
        MavenCli cli = new MavenCli();

        List<String> argList = new ArrayList<>();
        System.setProperty("maven.multiModuleProjectDirectory", srcLocation.getAbsolutePath());
        if (!runTests) {
            argList.add("-DskipTests=true");
        }
        argList.add("clean");
        argList.add("package");
        String[] args = new String[argList.size()];
        argList.toArray(args);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final String utf8 = StandardCharsets.UTF_8.name();
        try (PrintStream buffer = new PrintStream(baos, true, utf8)) {
            cli.doMain(args, srcLocation.getAbsolutePath(), buffer, buffer);
            String mvnOutput = baos.toString();
            LOG.debug(mvnOutput);
            if (mvnOutput.contains("BUILD SUCCESS")) {
                return true;
            }
            baos.close();
        } catch (UnsupportedEncodingException e) {
            LOG.error(e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public List<Pair<File, Patch<String>>> applyAndCompile(File srcLocation, List<Pair<File, Patch<String>>> patches, boolean runTests) {
        List<Pair<File, Patch<String>>> filteredPatches = new ArrayList<>();

        for (Pair<File, Patch<String>> patch : patches) {
            applyPatch(patch, srcLocation);
            if (compile(srcLocation, runTests)) {
                filteredPatches.add(patch);
            }
            revertPatch(patch, srcLocation);
        }

        return filteredPatches;
    }
}
