package eu.assuremoss.framework.model;

import lombok.Data;

import java.io.File;
import java.util.Map;

@Data
public class Patch {

    private String srcRootFolder;
    private Map<File, String> changedFiles;
    private String uniDiff;
}
