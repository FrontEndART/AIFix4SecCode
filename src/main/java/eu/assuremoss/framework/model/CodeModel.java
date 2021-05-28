package eu.assuremoss.framework.model;

import lombok.Data;

import java.io.File;

@Data
public class CodeModel {

    private MODEL_TYPES type;
    private File modelPath;

    public enum MODEL_TYPES {
        ASG,
        EMBEDDING,
        CFG,
        OSA_GRAPH
    }

    public CodeModel(MODEL_TYPES modelType, File codeModel) {
        this.type = modelType;
        this.modelPath = codeModel;
    }
}
