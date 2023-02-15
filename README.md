# AIFix4SecCode
An automated code repair framework for fixing vulnerabilities. The framework detects vulnerabilities based on static analysis with the help of the [OpenStaticAnalyzer](https://github.com/sed-inf-u-szeged/OpenStaticAnalyzer) tool that integrates [SpotBugs](https://spotbugs.github.io/) as well.

The detected vulnerabilities get automatically patched by an ASG transformation based repair solution implemented in the [CodeRepair](https://github.com/FrontEndART/OpenStaticAnalyzer/tree/CodeRepairTool/java/cl/CodeRepair) code module. Currently, the automatic repair of the following security issues are supported:
* [EI_EXPOSE_REP2](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ei2-may-expose-internal-representation-by-incorporating-reference-to-mutable-object-ei-expose-rep2)
* [EI_EXPOSE_REP] (https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ei-may-expose-internal-representation-by-returning-reference-to-mutable-object-ei-expose-rep)
* [MS_SHOULD_BE_FINAL](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ms-field-isn-t-final-but-should-be-ms-should-be-final)
* [NP_NULL_ON_SOME_PATH](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#np-possible-null-pointer-dereference-np-null-on-some-path)
* [NP_NULL_ON_SOME_PATH_EXCEPTION] (https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#np-possible-null-pointer-dereference-in-method-on-exception-path-np-null-on-some-path-exception)
* [MS_PKGPROTECT] (https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ms-field-should-be-package-protected-ms-pkgprotect)
* [MS_MUTABLE_COLLECTION] (https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ms-field-is-a-mutable-collection-ms-mutable-collection)
* [FI_PUBLIC_SHOULD_BE_PROTECTED] (https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#fi-finalizer-should-be-protected-not-public-fi-public-should-be-protected)



## How to install
You can install the framework by cloning its GitHub repository and building it with Maven:
```
git clone https://github.com/FrontEndART/AIFix4SecCode.git
cd AIFix4SecCode
mvn install:install-file -Dfile=src\\main\\resources\\CodeRepair-1.0.3-SNAPSHOT-jar-with-dependencies.jar -DgroupId=com.fea -DartifactId=coderepair -Dversion=1.0.1 -Dpackaging=jar -DgeneratePom=true
mvn package
cd vscode-plugin
npm install
```
Npm is installed with Node.js. This means that you have to install Node.js to get npm installed on your computer. You can download it from here: https://nodejs.org/

Next, you need to download and extract the [latest release](https://github.com/sed-inf-u-szeged/OpenStaticAnalyzer/releases) of the OpenStaticAnalyzer.


## How to use
Once the framework and the OpenStaticAnalyzer components are installed, you can use the framework by running the ``VulnerabilityRepairDriver`` main class. The program does not require any command-line parameters as it reads all the necessary information from the ``config.properties`` file located in the ``resources`` folder or the one provided via the command line. Therefore, you need to edit this file and enter appropriate data:

```
# General settings

config.project_name=NAME OF THE PROJECT # e.g. test-project
config.project_path=ABSOLUTE PATH TO THE PROJECT SRC # e.g. d:\\AIFix4SecCode\\test-project
config.project_source_path=RELATIVE PATH OF THE SOURCE FILES # e.g. src\\main\\java
config.project_build_tool=NAME OF THE BUILD TOOL # maven / mavenCLI / gradle / ant
config.project_run_tests=RUN UNIT TESTS OR NOT # true or false
config.results_path=FOLDER TO PUT ANALYSIS RESULTS # e.g. d:\\AIFix4SecCode\\test-project\\results
config.validation_results_path=FOLDER TO PUT VALIDATION ANALYSIS RESULTS # e.g. d:\\AIFix4SecCode\\test-project\\validation
config.archive_enabled=ARCHIVE THE GENERATED RESULTS OR NOT # true or false
config.archive_path=FOLDER TO PUT ARCHIVED DATA # e.g. d:\\AIFix4SecCode\\test-project\\archive
config.jsons_listfile=THE PATH OF THE RESULTED LIST FILE # e.g. d:\\AIFix4SecCode\\test-project\\results\\json.list

#Analyzer settings
config.osa_path=PATH TO THE JAVA OPEN STATIC ANALYZER # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java
config.osa_edition=ANALYZER EDITION # SourceMeter or OpenStaticAnalyzer
config.spotbugs_bin=PATH TO THE SPOTBUGS ANALYZER #e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java\\WindowsWrapper\\WrapperBins\\Tools\\spotbugs\\bin\\spotbugs.bat
config.jan_path=PATHS TO THE JAN ANALYZER # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java\\WindowsWrapper\\WrapperBins\\Tools
config.jan_edition="JAN.jar"
config.jan_compiler="jdk.compiler.jar"
config.additional_tools_path=PATHS TO THE OTHER AUXILIARY TOOLS # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java\\WindowsTools
config.jan2changepath_edition=JAN2ChangePath


# Repair strategies
strategy.EI_EXPOSE_REP2=EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2
strategy.EI_EXPOSE_REP=EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2
strategy.MS_SHOULD_BE_FINAL=MS_SHOULD_BE_FINAL
strategy.NP_NULL_ON_SOME_PATH=NP_NULL_ON_SOME_PATH
strategy.NP_NULL_ON_SOME_PATH_EXCEPTION=NP_NULL_ON_SOME_PATH_EXCEPTION
strategy.FI_PUBLIC_SHOULD_BE_PROTECTED=FI_PUBLIC_SHOULD_BE_PROTECTED
strategy.MS_PKGPROTECT=MS_PKGPROTECT
strategy.MS_MUTABLE_COLLECTION=MS_MUTABLE_COLLECTION

# Repair strategy descriptions
desc.EI_EXPOSE_REP2_ARRAY=Repair with Arrays.copyOf
desc.EI_EXPOSE_REP2_DATEOBJECT=Repair with creating new Date
desc.EI_EXPOSE_REP2=Repair with clone
desc.MS_SHOULD_BE_FINAL=Repair with adding final
desc.NP_NULL_ON_SOME_PATH=Repair with null-check in ternary
desc.NP_NULL_ON_SOME_PATH_EXCEPTION=Repair with null-check in ternary
desc.FI_PUBLIC_SHOULD_BE_PROTECTED=Finalize method should be protected
desc.MS_PKGPROTECT=Field should be package protected
desc.MS_MUTABLE_COLLECTION=Field is a mutable collection
```
If you would still work from a separate config file, use the -config=CONFIG_FILE_PATH command line argument. 
Instead of analyzing an entire project, it is also possible to analyze a single compilation unit, in this case use the -cu=COMPILATION_UNIT_PATH command line argument.

## How to install the plugin to VSCode
You can install the plugin to visual studio code from the command line:
```
code --install-extension <extension-vsix-path>
```
You can find the .vsix file in the vscode-plugin subdirectory of the project e.g. d:\\AIFix4SecCode\\vscode-plugin\\aifix4seccode-vscode-1.0.26.vsix

## Acknowledgement
The development of the AIFix4SecCode framework was supported by the [AssureMOSS](https://assuremoss.eu) (Grant No.952647) EU-funded project.
