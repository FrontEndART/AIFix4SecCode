package eu.assuremoss.utils.parsers;

import eu.assuremoss.VulnRepairDriver;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.Configuration;
import eu.assuremoss.utils.PathHandler;
import eu.assuremoss.utils.Utils;
import eu.assuremoss.utils.tools.JANAnalyser;
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
    boolean needAnalysis;
    Properties properties;
    HashMap<String, ASGInfoParser> cuParsers;
    String asgDir;
    PathHandler path;

    public SpotBugsParser(PathHandler path, Properties properties, File projectASG){
        models = new ArrayList<>();
        models.add(new CodeModel(CodeModel.MODEL_TYPES.SPOTBUGS_XML, new File(path.spotbugsXML(false))));
        this.path = path;
        this.properties = properties;
        projectPath =Paths.get(properties.getProperty(PROJECT_PATH_KEY), properties.getProperty(PROJECT_SOURCE_PATH_KEY)).toString();
        this.needAnalysis = projectASG==null?true:false;

        supportedProblemTypes = Utils.getSupportedProblemTypes(properties);

        if (projectASG != null)
            asgParser = new ASGInfoParser(projectASG);
        else cuParsers = new HashMap<>();
    }

    public SpotBugsParser(PathHandler path, Properties properties){
        this(path, properties, null);
        this.asgDir = path.asgDir();
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

    /***
     * @return it returns true, if the quality of the new entry set is better than the original one in the point of view of the current vulnerability entry
     */

    public boolean checkQualityImprovements(String compilationUnit, VulnerabilityEntry currentEntry, List<VulnerabilityEntry> newEntries) {
        if (newEntries == null || newEntries.isEmpty())
            return true;
        Set<VulnerabilityEntry> oldEntries = existingSpotBugIssues.get(compilationUnit);
        if ((oldEntries == null || oldEntries.isEmpty()) && newEntries != null) {
            return false;
        }
        if (!newEntries.contains(currentEntry)) {
            return true;
        }
        return false;
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

    public List<VulnerabilityEntry> readXML(boolean isValidation, boolean needColumnInfo) throws DataFormatException {
        List<VulnerabilityEntry> vulnerabilities = new ArrayList<>();
        var bugInstances = attributes(getNodeList(path.spotbugsXML(isValidation), "BugInstance"));
        for (Node bugInstance : bugInstances) {
            String bugType = getNodeAttribute(bugInstance, "type");
            if (!supportedProblemTypes.contains(bugType)) continue;
            VulnerabilityEntry entry = new VulnerabilityEntry();
            readBugInstanceInfo (entry, bugInstance);
            if (needAnalysis && needColumnInfo) {
                String className = entry.getClassName();
                if (!cuParsers.containsKey(className)) {
                    JANAnalyser jan = new JANAnalyser(properties, asgDir);
                    String asg = jan.analyze(entry.getPath(), entry.getClassName());
                    asgParser = new ASGInfoParser(new File(asg));

                    cuParsers.put(className, asgParser);
                } else {
                    asgParser = cuParsers.get(className);
                }
            }
            if (needColumnInfo)
                readColumnInfo(entry);
            setEntry(entry.getClassName(), entry);
            vulnerabilities.add(entry);
        }

        return vulnerabilities;
    }
}
