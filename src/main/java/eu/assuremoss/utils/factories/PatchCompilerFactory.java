package eu.assuremoss.utils.factories;

import eu.assuremoss.framework.api.PatchCompiler;
import eu.assuremoss.framework.modules.compiler.AntCLIPatchCompiler;
import eu.assuremoss.framework.modules.compiler.GradleCLIPatchCompiler;
import eu.assuremoss.framework.modules.compiler.MavenCLIPatchCompiler;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;

import java.security.InvalidParameterException;

public class PatchCompilerFactory {

    public static PatchCompiler getPatchCompiler(String buildTool) {
        switch (buildTool) {
            case "maven":
                return new MavenPatchCompiler();
            case "mavenCLI":
                return new MavenCLIPatchCompiler();
            case "gradle":
                return new GradleCLIPatchCompiler();
            case "ant":
                return new AntCLIPatchCompiler();
            default:
                throw new InvalidParameterException("ERROR: config.project_build_tool= " + buildTool + " - unknown build tool");
        }
    }
}
