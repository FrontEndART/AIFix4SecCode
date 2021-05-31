package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import org.apache.maven.cli.MavenCli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenPatchCompiler extends GenericPatchCompiler {
    @Override
    public List<Patch<String>> applyAndCompile(File srcLocation, List<Patch<String>> patches, boolean runTests) {
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
        for (Patch<String> patch : patches) {
            applyPatch(patch, srcLocation);
            cli.doMain(args, srcLocation.getAbsolutePath(), System.out, System.out);
            revertPatch(patch, srcLocation);
        }

        return patches;
    }
}
