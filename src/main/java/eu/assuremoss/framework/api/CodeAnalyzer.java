package eu.assuremoss.framework.api;

import eu.assuremoss.framework.model.CodeModel;

import java.io.File;
import java.util.List;

public interface CodeAnalyzer {

    public List<CodeModel> analyzeSourceCode(File srcLocation, boolean isValidation);
}
