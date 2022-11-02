package eu.assuremoss.utils.parsers;

import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.Utils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Configuration.PROJECT_PATH_KEY;
import static eu.assuremoss.utils.Configuration.PROJECT_SOURCE_PATH_KEY;
import static eu.assuremoss.utils.Utils.*;

public class SpotBugsParser {
    Map<String, Set<VulnerabilityEntry>> existingSpotBugIssues = new HashMap();
    public final Set<String> supportedProblemTypes;
    List<CodeModel> models;
    ASGInfoParser asgParser;
    String projectPath;

    public SpotBugsParser(List<CodeModel> models, Configuration config){
        projectPath =Paths.get(config.properties.getProperty(PROJECT_PATH_KEY), config.properties.getProperty(PROJECT_SOURCE_PATH_KEY)).toString();
        //System.out.println("Project path: " + projectPath);
        this.models = models;
        supportedProblemTypes = Utils.getSupportedProblemTypes(config.properties);
        try {
            asgParser = new ASGInfoParser(Utils.getCodeModel(models, CodeModel.MODEL_TYPES.ASG).get().getModelPath());
        } catch (DataFormatException e) {
            e.printStackTrace();
        }
    }

    private void setEntry(String compilationUnit, VulnerabilityEntry entry) {
        if (existingSpotBugIssues.containsKey(compilationUnit)) {
            existingSpotBugIssues.get(compilationUnit).add(entry);
        }  else {
            Set<VulnerabilityEntry> mySet = new HashSet<VulnerabilityEntry>();
            mySet.add(entry);
            existingSpotBugIssues.put(compilationUnit, mySet);
        }
    }

    private void removeEntriesOfCompilationUnit(String compilationUnit) {
        existingSpotBugIssues.remove(compilationUnit);
    }

    private  List<Node> attributes(NodeList nodeList) {
        return nodeListToArrayList(nodeList);
    }

    private void addKnownVariableName(VulnerabilityEntry entry) {
        if ("FI_PUBLIC_SHOULD_BE_PROTECTED".equals(entry.getType()))
            entry.setVariable("finalize");
    }

    private void readColumnInfo(VulnerabilityEntry entry)  {
        if (asgParser == null) return;
        asgParser.vulnarabilityInfoClarification(entry);

    }

    private void readBugInstanceInfo(VulnerabilityEntry entry, Node bugInstance) {
        NodeList warnAttributes = bugInstance.getChildNodes();
        entry.setType(bugInstance.getAttributes().getNamedItem("type").getNodeValue());
        for (int j = 0; j < warnAttributes.getLength(); j++) {
            if (warnAttributes.item(j).getAttributes() != null) {
                Node node =   warnAttributes.item(j);

                switch(node.getNodeName()) {
                    case "ShortMessage":
                        entry.setDescription(node.getTextContent());
                        break;
                    case "Class":
                        entry.setClassName(node.getAttributes().getNamedItem("classname").getNodeValue());
                        break;
                    case "LocalVariable":
                    case "Field":
                        entry.setVariable(node.getAttributes().getNamedItem("name").getNodeValue());
                        break;
                    case "SourceLine":
                        if (entry.getPath() == null) {
                            entry.setPath(Paths.get(projectPath, node.getAttributes().getNamedItem("sourcepath").getNodeValue()).toString().replaceAll("/", Matcher.quoteReplacement(File.separator)));
                            entry.setStartLine(Integer.parseInt(node.getAttributes().getNamedItem("start").getNodeValue()));
                            entry.setEndLine(Integer.parseInt(node.getAttributes().getNamedItem("end").getNodeValue()));
                        }
                        break;
                }
            }
        }
        addKnownVariableName (entry);
    }

    public List<VulnerabilityEntry> readXML() throws DataFormatException {
        Optional<CodeModel> spotBugs = Utils.getCodeModel(models, CodeModel.MODEL_TYPES.SPOTBUGS_XML);
        List<VulnerabilityEntry> vulnerabilities = new ArrayList<>();
        if (spotBugs.get().getType() != CodeModel.MODEL_TYPES.SPOTBUGS_XML) return vulnerabilities;

        var bugInstances = attributes(getNodeList(spotBugs, "BugInstance"));
        for (Node bugInstance : bugInstances) {
            String bugType = getNodeAttribute(bugInstance, "type");
            if (!supportedProblemTypes.contains(bugType)) continue;
            VulnerabilityEntry entry = new VulnerabilityEntry();
            readBugInstanceInfo (entry, bugInstance);
            readColumnInfo(entry);

            vulnerabilities.add(entry);
        }

        return vulnerabilities;
    }
}
