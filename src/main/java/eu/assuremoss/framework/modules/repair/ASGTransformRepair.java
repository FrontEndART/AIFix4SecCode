package eu.assuremoss.framework.modules.repair;

import coderepair.communication.base.RepairAlgorithmRunner;
import coderepair.repair.RepairToolSwitcher;
import com.github.difflib.UnifiedDiffUtils;
import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.MLogger;
import eu.assuremoss.utils.Pair;
import lombok.AllArgsConstructor;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static eu.assuremoss.utils.Configuration.PROJECT_SOURCE_PATH_KEY;

@AllArgsConstructor
public class ASGTransformRepair implements VulnerabilityRepairer {
    private static final Logger LOG = LogManager.getLogger(ASGTransformRepair.class);

    private static int PATCH_COUNTER = 1;

    private final String projectPath;
    private final String resultsPath;
    private final String descriptionPath;
    private final String patchSavePath;
    private final Map<String, Map<String, String>> fixStrategies;
    private final Properties properties;
    private final String jsiName;

    private File generateDescription(VulnerabilityEntry vulnerabilityEntry, String strategy) {
        File descriptionLocation = new File(descriptionPath);
        File descriptionFile = new File(descriptionLocation.getAbsolutePath(), "description.xml");

        Document document = new Document();
        Element root = new Element("linked-hash-map");

        Element entry1 = createEntryElement();
        entry1.addContent(createStringElement().addContent("coderepair.algs.problemType"));
        entry1.addContent(createStringElement().addContent(strategy));

        Element entry2 = createEntryElement();
        entry2.addContent(createStringElement().addContent("coderepair.algs.problemLocations"));
        Element list = new Element("list");
        list.addContent(createProblemToRepairElement(vulnerabilityEntry));
        entry2.addContent(list);
        String path = vulnerabilityEntry.getPath();
        Element entry3 = createEntryElement();
        entry3.addContent(createStringElement().addContent("coderepair.base.sourceCodeLocation"));
        entry3.addContent(createStringElement().addContent((
                Paths.get(projectPath,  properties.getProperty(PROJECT_SOURCE_PATH_KEY)) + File.separator)
                .replaceAll("\\\\", "/")));

        Element entry4 = createEntryElement();
        entry4.addContent(createStringElement().addContent("coderepair.base.schemaLocation"));

        entry4.addContent(createStringElement().addContent(jsiName.replaceAll("\\\\", "/")));

        root.addContent(entry1);
        root.addContent(entry2);
        root.addContent(entry3);
        root.addContent(entry4);

        document.setRootElement(root);

        if (!descriptionLocation.exists()) {
            try {
                Files.createDirectory(Paths.get(descriptionLocation.getPath()));
            } catch (IOException e) {
                LOG.error("Failed to create directory for the description.");
            }
        }
        XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat());
        try {
            out.output(document, new FileWriter(descriptionFile));
        } catch (IOException e) {
            LOG.error("Failed to save the description.");
        }

        return descriptionFile;
    }

    private Element createEntryElement() {
        return new Element("entry");
    }

    private Element createStringElement() {
        return new Element("string");
    }

    private Element createProblemToRepairElement(VulnerabilityEntry ve) {
        Element problemToRepair = new Element("coderepair.communication.base.ProblemToRepair");
        Element positions = new Element("positions");

        Element problemPosition = new Element("coderepair.communication.base.ProblemPosition");

        String pathToRemove = Paths.get(projectPath, properties.getProperty(PROJECT_SOURCE_PATH_KEY)) + File.separator;
        problemPosition.addContent(new Element("path").addContent(ve.getPath().substring(pathToRemove.length()).replaceAll("\\\\", "/")));

        problemPosition.addContent(new Element("startLine").addContent(ve.getStartLine() + ""));
        problemPosition.addContent(new Element("startCol").addContent(ve.getStartCol() + "")); //ve.getType().startsWith("NP") ? 20 : 24) + ""));
        problemPosition.addContent(new Element("endLine").addContent(ve.getEndLine() + ""));
        problemPosition.addContent(new Element("endCol").addContent(ve.getEndCol() + "")); //(ve.getType().startsWith("NP") ? 24 : 47) + ""));

        positions.addContent(problemPosition);
        problemToRepair.addContent(positions);

        return problemToRepair;
    }

    @Override
    public List<Pair<File, Pair<Patch<String>, String>>> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels)  {
        List<Pair<File, Pair<Patch<String>, String>>> resList = new ArrayList<>();
        for (String strategy : fixStrategies.get(ve.getType()).keySet()) {
            generateDescription(ve, strategy);

            String patchPath = Paths.get(patchSavePath, "repair_patch" + PATCH_COUNTER++ + ".diff").toString();
            String[] args = {
                    String.valueOf(Paths.get(descriptionPath, "description.xml")),
                    patchPath,
                    String.valueOf(Paths.get(resultsPath, "error.txt")),
            };
            RepairAlgorithmRunner rar = new RepairAlgorithmRunner();

            MLogger logger = MLogger.getActiveLogger();
            PrintStream console = System.out;
            System.setOut(logger.getFileWriter());
            try {
                rar.run(args, new RepairToolSwitcher());
                List<String> patchLines = Files.readAllLines(Path.of(patchPath));
                Patch<String> patch = UnifiedDiffUtils.parseUnifiedDiff(patchLines);

                if (!patch.getDeltas().isEmpty()) {
                    resList.add(new Pair<>(
                            new File(ve.getPath()),
                            new Pair<>(patch, fixStrategies.get(ve.getType()).get(strategy))
                    ));
                } else {
                    File emptyPatch = new File(patchPath);
                    if (emptyPatch.delete()) {
                        logger.fInfo((patchPath + " EMPTY PATCH deleted successfully!"));
                        PATCH_COUNTER--;
                    } else logger.info("Error occurred while deleting empty patch: " + patchPath);
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
            System.setOut(console);
        }
        return resList;
    }
}
