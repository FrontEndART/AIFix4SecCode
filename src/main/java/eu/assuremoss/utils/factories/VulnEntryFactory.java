package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.ColumnInfoParser;
import eu.assuremoss.utils.Pair;
import org.w3c.dom.NodeList;

public class VulnEntryFactory {
    public static VulnerabilityEntry getVulnEntry(NodeList warnAttributes, String problemType, String variable, String vulnType) {
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

        vulnEntry.setType(problemType);
        vulnEntry.setVulnType(vulnType);
        vulnEntry.setVariable(variable);

        Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfo(vulnEntry);

        vulnEntry.setStartCol(columnInfo.getA());
        vulnEntry.setEndCol(columnInfo.getB());

        return vulnEntry;
    }
}
