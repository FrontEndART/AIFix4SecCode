package eu.assuremoss.framework.modules.extractor;

import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.utils.ColumnInfoParser;
import eu.assuremoss.utils.Pair;

import java.util.List;

public class ColumnInfoExtractor {

    public void attachColumnInfo(List<VulnerabilityEntry> vulnerabilityLocations) {
        vulnerabilityLocations.forEach(vulnEntry -> {
            Pair<Integer, Integer> columnInfo = ColumnInfoParser.getColumnInfoFromFindBugsXML(vulnEntry);

            vulnEntry.setStartCol(columnInfo.getA());
            vulnEntry.setEndCol(columnInfo.getB());
        });
    }
}
