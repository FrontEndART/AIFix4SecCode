package eu.assuremoss.framework.modules.compiler;

import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.model.Patch;
import org.apache.maven.cli.MavenCli;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MavenPatchCompiler extends GenericPatchCompiler {
    @Override
    public List<Patch> applyAndCompile(File srcLocation, List<Patch> patches, boolean runTests) {
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
        for (Patch patch : patches) {
            applyPatch(patch, srcLocation);
            cli.doMain(args, srcLocation.getAbsolutePath(), System.out, System.out);
            revertPatch(patch, srcLocation);
        }

        return patches;
    }
}
