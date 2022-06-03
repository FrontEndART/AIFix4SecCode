# AIFix4SecCode
An automated code repair framework for fixing vulnerabilities. The framework detects vulnerabilities based on static analysis with the help of the [OpenStaticAnalyzer](https://github.com/sed-inf-u-szeged/OpenStaticAnalyzer) tool that integrates [SpotBugs](https://spotbugs.github.io/) as well.

The detected vulnerabilities get automatically patched by an ASG transformation based repair solution implemented in the [CodeRepair](https://github.com/FrontEndART/OpenStaticAnalyzer/tree/CodeRepairTool/java/cl/CodeRepair) code module. Currently, the automatic repair of the following security issues are supported:
* [EI_EXPOSE_REP2](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ei2-may-expose-internal-representation-by-incorporating-reference-to-mutable-object-ei-expose-rep2)
* [MS_SHOULD_BE_FINAL](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#ms-field-isn-t-final-but-should-be-ms-should-be-final)
* [NP_NULL_ON_SOME_PATH](https://spotbugs.readthedocs.io/en/stable/bugDescriptions.html#np-possible-null-pointer-dereference-np-null-on-some-path)

## How to install
You can install the framework by cloning its GitHub repository and building it with Maven:
```
git clone https://github.com/FrontEndART/AIFix4SecCode.git
cd AIFix4SecCode
mvn install:install-file -Dfile=src\\main\\resources\\CodeRepair-1.0-SNAPSHOT-jar-with-dependencies.jar -DgroupId=com.fea -DartifactId=coderepair -Dversion=1.0 -Dpackaging=jar -DgeneratePom=true
mvn package
cd vscode-plugin
npm install
```
Npm is installed with Node.js. This means that you have to install Node.js to get npm installed on your computer. You can download it from here: https://nodejs.org/

Next, you need to download and extract the [latest release](https://github.com/sed-inf-u-szeged/OpenStaticAnalyzer/releases) of the OpenStaticAnalyzer.


## How to use
Once the framework and the OpenStaticAnalyzer components are installed, you can use the framework by running the ``VulnRepairDriver`` main class. The program does not require any command-line parameters as it reads all the necessary information from the ``config.properties`` file located in the ``resources`` folder or the one provided via the command line. Therefore, you need to edit this file and enter appropriate data:

```
# General settings

config.project_name=NAME OF THE PROJECT # e.g. test-project
config.project_path=ABSOLUTE PATH TO THE PROJECT SRC # e.g. d:\\AIFix4SecCode\\test-project
config.project_build_tool=NAME OF THE BUILD TOOL # maven / mavenCLI / gradle / ant
config.osa_path=PATH TO THE JAVA OPEN STATIC ANALYZER # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java
config.osa_edition=ANALYZER EDITION # SourceMeter or OpenStaticAnalyzer
config.results_path=FOLDER TO PUT ANALYSIS RESULTS # e.g. d:\\AIFix4SecCode\\test-project\\results
config.validation_results_path=FOLDER TO PUT VALIDATION ANALYSIS RESULTS # e.g. d:\\AIFix4SecCode\\test-project\\validation
config.archive_enabled=ARCHIVE THE GENERATED RESULTS OR NOT # true or false
config.archive_path=FOLDER TO PUT ARCHIVED DATA # e.g. d:\\AIFix4SecCode\\test-project\\archive

# OSA to SpotBugs warning type mapping
mapping.FB_EiER=EI_EXPOSE_REP2
mapping.FB_EER=EI_EXPOSE_REP2
mapping.FB_MSBF=MS_SHOULD_BE_FINAL
mapping.FB_NNOSP=NP_NULL_ON_SOME_PATH
mapping.FB_NNOSPE=NP_NULL_ON_SOME_PATH_EXCEPTION

# Repair strategies
strategy.EI_EXPOSE_REP2=EI_EXPOSE_REP2_ARRAY|EI_EXPOSE_REP2_DATEOBJECT|EI_EXPOSE_REP2
strategy.MS_SHOULD_BE_FINAL=MS_SHOULD_BE_FINAL
strategy.NP_NULL_ON_SOME_PATH=NP_NULL_ON_SOME_PATH
strategy.NP_NULL_ON_SOME_PATH_EXCEPTION=NP_NULL_ON_SOME_PATH_EXCEPTION

# Repair strategy descriptions
desc.EI_EXPOSE_REP2_ARRAY=Repair with Arrays.copyOf
desc.EI_EXPOSE_REP2_DATEOBJECT=Repair with creating new Date
desc.EI_EXPOSE_REP2=Repair with clone
desc.MS_SHOULD_BE_FINAL=Repair with adding final
desc.NP_NULL_ON_SOME_PATH=Repair with null-check in ternary
desc.NP_NULL_ON_SOME_PATH_EXCEPTION=Repair with null-check in ternary
```

## How to install the plugin to VSCode
You can install the plugin to visual studio code from the command line:
```
code --install-extension <extension-vsix-path>
```
You can find the .vsix file in the vscode-plugin subdirectory of the project e.g. d:\\AIFix4SecCode\\vscode-plugin\\aifix4seccode-vscode-1.0.0.vsix

## Acknowledgement
The development of the AIFix4SecCode framework was supported by the [AssureMOSS](https://assuremoss.eu) (Grant No.952647) EU-funded project.
