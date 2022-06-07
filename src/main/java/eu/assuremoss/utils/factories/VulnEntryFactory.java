package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.ColumnInfoParser;
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
    public static final Map<String, String> supportedProblemTypes = Utils.getMappingConfig();

    public static VulnerabilityEntry getVulnEntry(Node node, Optional<CodeModel> findBugsXML) {
        NodeList warnAttributes = node.getChildNodes();

        VulnerabilityEntry vulnEntry = new VulnerabilityEntry();

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
        vulnEntry.setVariable(getVariable(node, findBugsXML));

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        vulnEntry.setStartCol(columnInfo.getA());
        vulnEntry.setEndCol(columnInfo.getB());

        return vulnEntry;
    }


    private static String getVariable(Node node, Optional<CodeModel> findBugsXML) {
        try {
//            System.out.println(supportedProblemTypes.get(nodeName(node)) + " " + lineNumStr(node));
            return findVariableInFindBugsXML(nodeName(node), lineNumStr(node), findBugsXML);
        } catch (ParserConfigurationException | SAXException | IOException | DataFormatException e) {
            e.printStackTrace();
            return "";
        }
    }

    private static String findVariableInFindBugsXML(String vulnType, String lineNum, Optional<CodeModel>  findBugsCM) throws ParserConfigurationException, SAXException, IOException, DataFormatException {
        if (findBugsCM.get().getType() != CodeModel.MODEL_TYPES.FINDBUGS_XML) return null;

        var bugInstances = attributes(getNodeList(findBugsCM, "BugInstance"));

        for (Node bugInstance : bugInstances) {
            String bugType = getNodeAttribute(bugInstance, "type");
            if (!supportedProblemTypes.containsKey(vulnType)) continue;
            if (!supportedProblemTypes.get(vulnType).equals(bugType)) continue;

            String foundLineNum = null;
            Node localVariable = null;

            List<Node> children = nodeListToArrayList(bugInstance.getChildNodes());
            for (Node child : children) {
                // Get line num
                switch (child.getNodeName()) {
                    case "SourceLine":
                        foundLineNum = getNodeAttribute(child, "start");
                        break;

                    case "LocalVariable":
                    case "Field":
                        localVariable = child;
                        break;
                }
            }

            // TODO: clean this up
            if (foundLineNum != null && localVariable == null) {
//                System.out.println("Found " + bugType + " on line " + foundLineNum + " without associated variable!");
                return null;
            }

            if (foundLineNum == null) continue;

            if (foundLineNum.equals(lineNum)) {
                return getNodeAttribute(localVariable, "name");
            }
        }

        return null;
    }


    private static String lineNumStr(Node node) {
        return getNodeAttribute(node.getChildNodes().item(3), "value");
    }

    private static List<Node> attributes(NodeList nodeList) {
        return nodeListToArrayList(nodeList);
    }

    private static String nodeName(Node node) {
        return getNodeAttribute(node, "name");
    }
}
