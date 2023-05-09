package eu.assuremoss.utils.patchPrioritizer;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.PathHandler;
import eu.assuremoss.utils.ProcessRunner;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeSimilarityAlgorithm implements PatchPrioritizeAlgorithm {

    private static final Logger LOG = LogManager.getLogger(CodeSimilarityAlgorithm.class);

    Map<String, String> vectorizers = Map.of(
            "word2vec", "word2vec",
            "glove", "glove",
            "binaryast", "binaryast",
            "doc2vec", "doc2vec",
            "token", "token",
            "bagofwords", "bagofwords"
    );
    Map<String, String> comparers = Map.of(
            "word2vec", "cossim",
            "glove", "triangle",
            "binaryast", "cossim",
            "doc2vec", "cossim",
            "token", "hamming",
            "bagofwords", "relativeeuclidean"
    );
    private final String prioritizerPath;
    private final String prioritizerMode;

    private final PathHandler path;
    public CodeSimilarityAlgorithm(String prioritizerPath, String prioritizerMode, PathHandler path){
        this.prioritizerPath = prioritizerPath;
        this.prioritizerMode = prioritizerMode;
        this.path = path;
    }


    private void writePatches(List<Pair<File, Pair<Patch<String>, String>>> patches, String patchesPath, String sorterDirectory){
        List<String> patchPaths = new ArrayList<>();
        for (int i = 0; i < patches.size(); i++) {
            String sourcePath = patches.get(i).getA().getAbsolutePath().toString();
            Patch<String> patch = patches.get(i).getB().getA();
            String patchSavePath = Paths.get(sorterDirectory, "prioritizer_patch_" + i + ".diff").toString();
            patchPaths.add(patchSavePath);
            try (PrintWriter patchWriter = new PrintWriter(patchSavePath)) {
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(sourcePath, sourcePath,
                                Arrays.asList(Files.readString(Path.of(sourcePath)).split("\n")), patch, 2);

                String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                patchWriter.write(diffString);
            } catch (IOException e) {
                LOG.error("Error during saving patch for prioritizer " + i);
            }
        }
        try (PrintWriter patchWriter = new PrintWriter(patchesPath)) {
            patchWriter.write(Joiner.on("\n").join(patchPaths) + "\n");
        } catch (IOException e) {
            LOG.error("Error during saving patches for prioritizer ");
        }
    }

    private Map<Integer, Double> readResults(String prioritizerResultsPath) throws FileNotFoundException {
        Map<Integer, Double> result = new HashMap<>();
        Scanner sc = new Scanner(new File(prioritizerResultsPath));
        Pattern pattern = Pattern.compile("prioritizer_patch_(\\d+)\\.diff");

        while(sc.hasNextLine()){
            String[] segments = sc.nextLine().split(";");
            Matcher matcher= pattern.matcher(segments[0]);
            if (matcher.find()){
                result.put(
                        Integer.parseInt(matcher.group(0).substring("prioritizer_patch_".length(),
                                        matcher.group(0).length()-".diff".length())),
                        Double.parseDouble(segments[1])
                );
            }

        }
        return result;
    }

    private void deleteTemporaryFiles(File sorterDirectory){
        File[] files = sorterDirectory.listFiles();
        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
        sorterDirectory.delete();
    }

    private String resolveVectorizer(String prioritizerMode){
        return vectorizers.getOrDefault(prioritizerMode, "word2vec");
    }
    private String resolveComparer(String prioritizerMode){
        return comparers.getOrDefault(prioritizerMode, "cossim");
    }

    private void runSorter(String patchesPath, String prioritizerResultsPaths){
        ProcessBuilder processBuilder = new ProcessBuilder("python", prioritizerPath, "--vectorizer", resolveVectorizer(prioritizerMode), "--comparer", resolveComparer(prioritizerMode), patchesPath, prioritizerResultsPaths);
        processBuilder.redirectErrorStream(true);
        ProcessRunner.run(processBuilder);
    }

    @Override
    public List<Pair<Pair<File, Pair<Patch<String>, String>>, Double>> prioritize(List<Pair<File, Pair<Patch<String>, String>>> patches) {
        List<Pair<Pair<File, Pair<Patch<String>, String>>, Double>> result = new ArrayList<>();
        String patchesPath = Path.of(path.getResultsPath(),"sorter", "patches.txt").toString();
        String prioritizerResultsPaths = Path.of(path.getResultsPath(),"sorter", "prioritizer_results.txt").toString();
        File sorterDirectory = new File(Path.of(path.getResultsPath(),"sorter").toString());
        if (!sorterDirectory.exists()) {
            sorterDirectory.mkdir();
        }
        writePatches(patches, patchesPath, sorterDirectory.getAbsolutePath());
        runSorter(patchesPath, prioritizerResultsPaths);
        Map<Integer, Double> scores = null;
        try {
            scores = readResults(prioritizerResultsPaths);
            for (int i = 0; i < patches.size(); i++) {
                result.add(new Pair<>(patches.get(i), scores.getOrDefault(i, 10.0)));
            }
        } catch (FileNotFoundException e) {
            for (Pair<File, Pair<Patch<String>, String>> pair : patches) {
                result.add(new Pair<>(pair, 10.0));
            }
        }
        //deleteTemporaryFiles(patchesPath, prioritizerResultsPaths);
        deleteTemporaryFiles(sorterDirectory);
        return result;
    }
}
