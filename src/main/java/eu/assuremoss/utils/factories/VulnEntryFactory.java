package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.parsers.ASGInfoParser;
import eu.assuremoss.utils.Pair;
import eu.assuremoss.utils.Utils;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.DataFormatException;

import static eu.assuremoss.utils.Utils.*;

public class VulnEntryFactory {
    public final Map<String, String> supportedProblemTypes = Utils.getMappingConfig();
    Optional<CodeModel> findBugsXML;
    Optional<CodeModel> asg;
    ASGInfoParser asgParser;

    public VulnEntryFactory(Optional<CodeModel> findBugsXML, Optional<CodeModel> asg) {
        this.findBugsXML = findBugsXML;
        this.asg = asg;
        asgParser = new ASGInfoParser(asg.get().getModelPath());
    }

    public  VulnerabilityEntry getVulnEntry(Node node) {
        NodeList warnAttributes = node.getChildNodes();

        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();

        // Get vulnerability information from Graph.xml
        for (int j = 0; j < warnAttributes.getLength(); j++) {
            if (warnAttributes.item(j).getAttributes() != null) {
                String attrType = warnAttributes.item(j).getAttributes().getNamedItem("name").getNodeValue();
                if ("ExtraInfo".equals(attrType)) {
                    continue;
                }
                String attrVal = warnAttributes.item(j).getAttributes().getNamedItem("value").getNodeValue();
                switch (attrType) {
                    case "Path":
                        vulnEntry.setPath(attrVal);
                        break;
                    case "Line":
                        vulnEntry.setStartLine(Integer.parseInt(attrVal));
                        break;
                    case "EndLine":
                        vulnEntry.setEndLine(Integer.parseInt(attrVal));
                        break;

                    case "WarningText":
                        vulnEntry.setDescription(attrVal);
                        break;
                }
            }
        }

        vulnEntry.setType(supportedProblemTypes.get(nodeName(node)));
        vulnEntry.setVulnType(getNodeAttribute(node, "name"));

        // Extract variable name from SpotBugs.xml

        vulnEntry.setVariable(getVariable(node, vulnEntry.getPath()));
       if (vulnEntry.getType().equals("FB_MSBF"))
          vulnEntry.setVariable("finalize");

        // Extract column info from SpotBugs.xml
        //Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);
        asgParser.vulnarabilityInfoClarification(vulnEntry);
        /*vulnEntry.setStartCol(columnInfo.getA());
        vulnEntry.setEndCol(columnInfo.getB());*/

        return vulnEntry;
    }


    private  String getVariable(Node node, String path) {
        try {
            return findVariableInFindBugsXML(nodeName(node), lineNumStr(node), path);
        } catch (ParserConfigurationException | SAXException | IOException | DataFormatException e) {
            e.printStackTrace();
            return "";
        }
    }

    private  String findVariableInFindBugsXML(String vulnType, String lineNum, String path) throws ParserConfigurationException, SAXException, IOException, DataFormatException {
        if (findBugsXML.get().getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return null;

        var bugInstances = attributes(getNodeList(findBugsXML, "BugInstance"));

        for (Node bugInstance : bugInstances) {
            String bugType = getNodeAttribute(bugInstance, "type");
            if (!supportedProblemTypes.containsKey(vulnType)) continue;
            if (!supportedProblemTypes.get(vulnType).equals(bugType)) continue;

            String foundLineNum = null;
            Node localVariable = null;
            String foundPath = "";

            // Get vulnerability information from FindBugs.xml
            List<Node> children = nodeListToArrayList(bugInstance.getChildNodes());
            for (Node child : children) {
                switch (child.getNodeName()) {
                    case "SourceLine":
                        // if (isNodeRoleSourceLineEqualsKnownNull(child)) break;

                        foundLineNum = getNodeAttribute(child, "start");
                        foundPath = getNodeAttribute(child, "sourcepath");
                        break;

                    case "LocalVariable":
                    case "Field":
                        localVariable = child;
                        break;
                }
            }

            // Compare Graph.xml data with newly found data in FindBugs.xml
            if (foundLineNum != null && localVariable == null) return null;

            if (foundLineNum == null) continue;

            if (foundLineNum.equals(lineNum) && path.contains(foundPath)) {
                return getNodeAttribute(localVariable, "name");
            }
            //return getNodeAttribute(localVariable, "name");
        }

        return null;
    }



    private  String lineNumStr(Node node) {
        return getNodeAttribute(node.getChildNodes().item(3), "value");
    }

    private  List<Node> attributes(NodeList nodeList) {
        return nodeListToArrayList(nodeList);
    }

    private  String nodeName(Node node) {
        return getNodeAttribute(node, "name");
    }
}
