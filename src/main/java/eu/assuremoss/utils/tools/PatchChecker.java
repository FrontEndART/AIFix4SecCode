package eu.assuremoss.utils.tools;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.api.PatchValidator;
import eu.assuremoss.framework.api.SourceCodeCollector;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.factories.ToolFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;
import static eu.assuremoss.utils.Configuration.patchSavePath;

public class PatchChecker {
    private static final Logger LOG = LogManager.getLogger(PatchChecker.class);
    public static int patchCounter = 1;
    public List<Pair<File, Pair<Patch<String>, String>>>
    getCandidatePatches(Properties props,
                        SourceCodeCollector scc,
                        VulnerabilityEntry vulnEntry,
                        PatchCompiler comp,
                        List<Pair<File, Pair<Patch<String>, String>>> filteredPatches) {
        List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
        //PatchValidator patchValidator = ToolFactory.createOsa(props);

        for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : filteredPatches) {
            Patch<String> rawPatch = patchWithExplanation.getB().getA();
            Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);

            comp.applyPatch(patch, scc.getSourceCodeLocation());
            //if (patchValidator.validatePatch(scc.getSourceCodeLocation(), vulnEntry, patch)) {
                candidatePatches.add(patchWithExplanation);
            //}
            comp.revertPatch(patch, scc.getSourceCodeLocation());
        }

        return candidatePatches;
    }

    public JSONObject generateFixEntity(Properties props, VulnerabilityEntry vulnEntry, List<Pair<File, Pair<Patch<String>, String>>> candidatePatches) {
        JSONArray patchesArray = new JSONArray();
        JSONObject issueObject = new JSONObject();
        for (int i = 0; i < candidatePatches.size(); i++) {
            File path = candidatePatches.get(i).getA();
            Patch<String> patch = candidatePatches.get(i).getB().getA();
            String explanation = candidatePatches.get(i).getB().getB();

            // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
            String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}.diff", patchCounter++, vulnEntry.getType(), vulnEntry.getStartLine(), vulnEntry.getEndLine(), vulnEntry.getStartCol(), vulnEntry.getEndCol());
            try (PrintWriter patchWriter = new PrintWriter(String.valueOf(Paths.get(patchSavePath(props), patchName)))) {
                List<String> unifiedDiff =
                        UnifiedDiffUtils.generateUnifiedDiff(path.getPath(), path.getPath(),
                                Arrays.asList(Files.readString(Path.of(path.getAbsolutePath())).split("\n")), patch, 2);

                // make the path in the patch file relative to the project path
                for (int j = 0; j < 2; j++) {
                    String line = unifiedDiff.get(j);

                    String regex = props.getProperty(PROJECT_PATH_KEY);
                    regex = regex.replaceAll("\\\\", "\\\\\\\\");

                    String[] lineParts = line.split(regex);
                    if (lineParts[1].charAt(0) == '\\' || lineParts[1].charAt(0) == '/') {
                        lineParts[1] = lineParts[1].substring(1);
                    }

                    unifiedDiff.set(j, lineParts[0] + lineParts[1]);
                }

                String diffString = Joiner.on("\n").join(unifiedDiff) + "\n";
                patchWriter.write(diffString);

                JSONObject patchObject = new JSONObject();
                patchObject.put("path", patchName);
                patchObject.put("explanation", explanation);
                patchObject.put("score", 10);
                patchesArray.add(patchObject);
            } catch (IOException e) {
                LOG.error("Failed to save candidate patch: " + patch);
            }
        }

        JSONObject textRangeObject = new JSONObject();
        textRangeObject.put("startLine", vulnEntry.getStartLine());
        textRangeObject.put("endLine", vulnEntry.getEndLine());
        textRangeObject.put("startColumn", vulnEntry.getStartCol() - 1);
        textRangeObject.put("endColumn", vulnEntry.getEndCol() - 1);

        issueObject.put("patches", patchesArray);
        issueObject.put("textRange", textRangeObject);

        return issueObject;
    }
}
