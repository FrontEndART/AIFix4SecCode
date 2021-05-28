package eu.assuremoss.framework.api;

import java.io.File;

public interface SourceCodeCollector {

    public void collectSourceCode();

    public File getSourceCodeLocation();
}
