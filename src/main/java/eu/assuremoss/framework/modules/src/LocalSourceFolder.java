package eu.assuremoss.framework.modules.src;

import eu.assuremoss.framework.api.SourceCodeCollector;

import java.io.File;

public class LocalSourceFolder implements SourceCodeCollector {

    private String location;
    private File srcLocation;

    public LocalSourceFolder(String location) {
        this.location = location;
    }

    @Override
    public void collectSourceCode() {
        srcLocation = new File(location);
    }

    @Override
    public File getSourceCodeLocation() {
        return srcLocation;
    }
}
