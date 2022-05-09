package eu.assuremoss;

import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import com.google.common.base.Joiner;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import eu.assuremoss.framework.api.*;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.factories.ToolFactory;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static eu.assuremoss.utils.Configuration.*;
import static eu.assuremoss.utils.Utils.getConfigFile;


/**
 * The main driver class that runs the vulnerability repair workflow
 */
public class VulnRepairDriver {
    private static final Logger LOG = LogManager.getLogger(VulnRepairDriver.class);

    public static void main(String[] args) throws IOException {
        VulnRepairDriver driver = new VulnRepairDriver();
        Configuration config = new Configuration(getConfigFile(args));

        driver.bootstrap(config.properties);
    }

    public void bootstrap(Properties props) {
        LOG.info("Start!");

        try {
            Files.createDirectory(Paths.get(props.getProperty(RESULTS_PATH_KEY)));
        } catch (IOException e) {
            LOG.info("Unable to create results folder.");
        }

        String currentTime = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss").format(new Date());

        SourceCodeCollector scc = new LocalSourceFolder(props.getProperty(PROJECT_PATH_KEY));
        scc.collectSourceCode();

        CodeAnalyzer osa = ToolFactory.createOsa(props);
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation(), false);
        codeModels.stream().forEach(cm -> LOG.debug(cm.getType() + ":" + cm.getModelPath()));

        VulnerabilityDetector vd = ToolFactory.createOsa(props);
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation(), codeModels);
        vulnerabilityLocations.forEach(ve -> System.out.println(ve.getType() + " -> " + ve.getStartLine()));

        VulnerabilityRepairer vr = ToolFactory.createASGTransformRepair(props);
        int patchCounter = 1;
        Map<String, Integer> problemTypeCounter = new HashMap<>();
        JSONObject vsCodeConfig = new JSONObject();

        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Pair<File, Pair<Patch<String>, String>>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            LOG.debug(String.valueOf(patches));
            PatchCompiler comp = new MavenPatchCompiler();
            List<Pair<File, Pair<Patch<String>, String>>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = ToolFactory.createOsa(props);

            List<Pair<File, Pair<Patch<String>, String>>> candidatePatches = new ArrayList<>();
            for (Pair<File, Pair<Patch<String>, String>> patchWithExplanation : filteredPatches) {
                Patch<String> rawPatch = patchWithExplanation.getB().getA();
                Pair<File, Patch<String>> patch = new Pair<>(patchWithExplanation.getA(), rawPatch);
                comp.applyPatch(patch, scc.getSourceCodeLocation());
                if (pv.validatePatch(scc.getSourceCodeLocation(), ve, patch)) {
                    candidatePatches.add(patchWithExplanation);
                }
                comp.revertPatch(patch, scc.getSourceCodeLocation());
            }
            File patchSavePathDir = new File(patchSavePath(props));
            if (!patchSavePathDir.exists()) {
                try {
                    Files.createDirectory(Paths.get(patchSavePath(props)));
                } catch (IOException e) {
                    LOG.error("Failed to create directory for patches.");
                }
            }
            LOG.debug(String.valueOf(candidatePatches));
            if (candidatePatches.isEmpty()) {
                continue;
            }
            JSONArray patchesArray = new JSONArray();
            for (int i = 0; i < candidatePatches.size(); i++) {
                File path = candidatePatches.get(i).getA();
                Patch<String> patch = candidatePatches.get(i).getB().getA();
                String explanation = candidatePatches.get(i).getB().getB();

                // Dump the patch and generate the necessary meta-info json as well with vulnerability/patch candidate mapping for the VS Code plug-in
                String patchName = MessageFormat.format("patch_{0}_{1}_{2}_{3}_{4}_{5}.diff", patchCounter++, ve.getType(), ve.getStartLine(), ve.getEndLine(), ve.getStartCol(), ve.getEndCol());
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
            JSONObject issueObject = new JSONObject();

            JSONObject textRangeObject = new JSONObject();
            textRangeObject.put("startLine", ve.getStartLine());
            textRangeObject.put("endLine", ve.getEndLine());
            textRangeObject.put("startColumn", ve.getStartCol() - 1);
            textRangeObject.put("endColumn", ve.getEndCol() - 1);

            issueObject.put("patches", patchesArray);
            issueObject.put("textRange", textRangeObject);

            String problemTypeCount;
            if (problemTypeCounter.get(ve.getType()) == null) {
                problemTypeCounter.put(ve.getType(), 1);
                problemTypeCount = "";
            } else {
                int n = problemTypeCounter.get(ve.getType());
                n++;
                problemTypeCounter.put(ve.getType(), n);
                problemTypeCount = "#" + n;
            }

            vsCodeConfig.put(ve.getType() + problemTypeCount, issueObject);
        }

        try (FileWriter fw = new FileWriter(String.valueOf(Paths.get(patchSavePath(props), "vscode-config.json")))) {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            JsonElement element = JsonParser.parseString(vsCodeConfig.toJSONString());
            fw.write(gson.toJson(element));
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (archiveEnabled(props)) {
            Utils.archiveResults(patchSavePath(props), props.getProperty(ARCHIVE_PATH), descriptionPath(props), currentTime);
        }

        Utils.deleteIntermediatePatches(patchSavePath(props));
    }
}
