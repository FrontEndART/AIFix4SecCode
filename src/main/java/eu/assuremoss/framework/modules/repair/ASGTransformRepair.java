package eu.assuremoss.framework.modules.repair;

import com.github.difflib.DiffUtils;
import com.github.difflib.patch.Patch;
import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.api.VulnerabilityRepairer;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Pair;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class ASGTransformRepair implements VulnerabilityRepairer {
    @Override
    public File generateDescription(File descriptionLocation, List<VulnerabilityEntry> vulnerabilityLocations) {
        File descriptionFile = new File(descriptionLocation.getPath(), "description.xml");

        Document document = new Document();
        Element root = new Element("linked-hash-map");

        Element entry1 = createEntryElement();
        entry1.addContent(createStringElement().addContent("coderepair.algs.problemType"));
        entry1.addContent(createStringElement().addContent("PMD_PLFIC"));

        Element entry2 = createEntryElement();
        entry2.addContent(createStringElement().addContent("coderepair.algs.problemLocations"));
        Element list = new Element("list");
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            list.addContent(createProblemToRepairElement(ve));
        }

        entry2.addContent(list);

        root.addContent(entry1);
        root.addContent(entry2);
        document.setRootElement(root);

        if (!descriptionLocation.exists()) {
            try {
                Files.createDirectory(Paths.get(descriptionLocation.getPath()));
            } catch (IOException e) {
                VulnRepairDriver.LOGGER.error("Failed to create directory for the description.");
            }
        }
        XMLOutputter out = new XMLOutputter();
        out.setFormat(Format.getPrettyFormat());
        try {
            out.output(document, new FileWriter(descriptionFile));
        } catch (IOException e) {
            VulnRepairDriver.LOGGER.error("Failed to save the description.");
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
        Element problemToRepair = new Element("coderepair.repairtools.base.ProblemToRepair");
        Element positions = new Element("positions");

        Element problemPosition = new Element("coderepair.repairtools.base.ProblemPosition");
        problemPosition.addContent(new Element("path").addContent(ve.getPath()));
        problemPosition.addContent(new Element("startLine").addContent(ve.getStartLine() + ""));
        problemPosition.addContent(new Element("startCol").addContent(ve.getStartCol() + ""));
        problemPosition.addContent(new Element("endLine").addContent(ve.getEndLine() + ""));
        problemPosition.addContent(new Element("endCol").addContent(ve.getEndCol() + ""));

        positions.addContent(problemPosition);
        problemToRepair.addContent(positions);

        return problemToRepair;
    }

    @Override
    public List<Pair<File, Patch<String>>> generateRepairPatches(File srcLocation, VulnerabilityEntry ve, List<CodeModel> codeModels) {
        List<Pair<File, Patch<String>>> resList = new ArrayList<>();
        // for testing purposes load two modifications of the test project and generate a valid and a wrong patch
        File asyncAppenderOrig = new File(srcLocation, String.valueOf(Paths.get("src", "main", "java", "vir", "samples", "HelloWorld.java")));
        List<String> text1 = new ArrayList<>();
        List<String> text2 = new ArrayList<>();
        List<String> text3 = new ArrayList<>();
        try {
            text1 = new BufferedReader(new FileReader(asyncAppenderOrig)).lines()
                    .collect(Collectors.toList());
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            InputStream resourceStream = loader.getResourceAsStream("HelloWorld-good.java");
            text2 = new BufferedReader(new InputStreamReader(resourceStream)).lines()
                    .collect(Collectors.toList());
            resourceStream = loader.getResourceAsStream("HelloWorld-bad.java");
            text3 = new BufferedReader(new InputStreamReader(resourceStream)).lines()
                    .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        //generating diff information.
        Patch<String> patchGood = DiffUtils.diff(text1, text2);
        Patch<String> patchBad = DiffUtils.diff(text1, text3);

        resList.add(new Pair<>(asyncAppenderOrig, patchGood));
        resList.add(new Pair<>(asyncAppenderOrig, patchBad));

        return resList;
    }
}
