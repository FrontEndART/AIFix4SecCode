package eu.assuremoss.framework.modules.compiler;

import com.github.difflib.patch.Patch;
import com.github.difflib.patch.PatchFailedException;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.utils.Pair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class GenericPatchCompiler implements PatchCompiler {
    public String buildDirectoryName;

    protected abstract void initBuildDirectoryName();

    protected GenericPatchCompiler() {
        initBuildDirectoryName();
    }

    @Override
    public List<Pair<File, Pair<Patch<String>, String>>> applyAndCompile(File srcLocation, List<Pair<File, Pair<Patch<String>, String>>> patches, boolean runTests) {
        List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = new ArrayList<>();

        for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : patches) {
            Patch<String> rawPatch = patchWithExplanation.getB().getA();
            Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);
            applyPatch(patch, srcLocation);
            if (compile(srcLocation, runTests, false)) {
                filteredPatches.add(patchWithExplanation);
            }
            revertPatch(patch, srcLocation);
        }

        return filteredPatches;
    }

    @Override
    public void revertPatch(Pair<File, Patch<String>> patch, File srcLocation) {
        try {
            List<String> file = Files.readAllLines(Paths.get(patch.getA().getPath()));

            List<String> restoredFile = patch.getB().restore(file);
            writeToFile(restoredFile, patch.getA());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void applyPatch(Pair<File, Patch<String>> patch, File srcLocation) {
        try {
            List<String> file = Files.readAllLines(Paths.get(patch.getA().getPath()));

            List<String> patchedFile = patch.getB().applyTo(file);
            writeToFile(patchedFile, patch.getA());
        } catch (IOException | PatchFailedException e) {
            e.printStackTrace();
        }
    }

    private void writeToFile(List<String> list, File file) throws IOException {
        FileWriter fw = new FileWriter(file);
        for(String s: list) {
            fw.write(s + System.lineSeparator());
        }
        fw.close();
    }

    public String getBuildDirectoryName() {
        return buildDirectoryName;
    }
}
