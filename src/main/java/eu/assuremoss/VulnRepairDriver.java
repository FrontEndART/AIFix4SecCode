package eu.assuremoss;

import com.github.difflib.patch.Patch;
import eu.assuremoss.framework.api.*;
import eu.assuremoss.framework.model.CodeModel;
import eu.assuremoss.framework.model.VulnerabilityEntry;
import eu.assuremoss.framework.modules.analyzer.OpenStaticAnalyzer;
import eu.assuremoss.framework.modules.compiler.MavenPatchCompiler;
import eu.assuremoss.framework.modules.repair.ASGTransformRepair;
import eu.assuremoss.framework.modules.src.LocalSourceFolder;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

/**
 * The main driver class that runs the vulnerability repair workflow
 *
 */
public class VulnRepairDriver
{
    public static void main( String[] args )
    {
        SourceCodeCollector scc = new LocalSourceFolder("c:/data/teaching/Alkalmazasfejlesztes-I/kotprog/sol/core_module");
        scc.collectSourceCode();
        CodeAnalyzer osa = new OpenStaticAnalyzer(".");
        List<CodeModel> codeModels = osa.analyzeSourceCode(scc.getSourceCodeLocation());
        codeModels.stream().forEach(cm -> System.out.println(cm.getType() + ":" + cm.getModelPath()));
        VulnerabilityDetector vd = new OpenStaticAnalyzer(".");
        List<VulnerabilityEntry> vulnerabilityLocations = vd.getVulnerabilityLocations(scc.getSourceCodeLocation());
        VulnerabilityRepairer vr = new ASGTransformRepair();
        for (VulnerabilityEntry ve : vulnerabilityLocations) {
            List<Patch<String>> patches = vr.generateRepairPatches(scc.getSourceCodeLocation(), ve, codeModels);
            System.out.println(patches);
            PatchCompiler comp = new MavenPatchCompiler();
            List<Patch<String>> filteredPatches = comp.applyAndCompile(scc.getSourceCodeLocation(), patches, true);
            PatchValidator pv = new OpenStaticAnalyzer(".");
            List<Patch<String>> candidatePatches = new ArrayList<>();
            for (Patch<String> patch : filteredPatches) {
                comp.applyPatch(patch, scc.getSourceCodeLocation());
                if (pv.validatePatch(scc.getSourceCodeLocation(), ve, patch)) {
                    candidatePatches.add(patch);
                }
                comp.revertPatch(patch, scc.getSourceCodeLocation());
            }
            // TODO: print out patches to files or pass them to a Visualizer
        }
    }
}
