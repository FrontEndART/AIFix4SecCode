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
```
Next, you need to download and extract the [latest release](https://github.com/sed-inf-u-szeged/OpenStaticAnalyzer/releases) of the OpenStaticAnalyzer.


## How to use
Once the framework and the OpenStaticAnalyzer components are installed, you can use the framework by running the ``VulnRepairDriver`` main class. The program does not require any command-line parameters as it reads all the necessary information from the ``config.properties`` file located in the ``resources`` folder. Therefore, you need to edit this file and enter appropriate data:

```
project_name=NAME OF THE PROJECT # e.g. test-project
project_path=ABSOLUTE PATH TO THE PROJECT SRC # e.g. d:\\AIFix4SecCode\\test-project
osa_path=PATH TO THE JAVA OPEN STATIC ANALYZER # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java
osa_edition=ANALYZER EDITION # SourceMeter or OpenStaticAnalyzer
results_path=FOLDER TO PUT ANALYSIS RESULTS # e.g. d:\\AIFix4SecCode\\test-project\\results
patch_save_path=FOLDER TO SAVE GENERATED FIX PATCHES # d:\\AIFix4SecCode\\test-projectq\results\\patches
description_path=FOLDER TO SAVE REPAIR DESCRIPTION XML # e.g. d:\\AIFix4SecCode\\test-project\\results\\osa_xml
j2cp_path=PATH OF THE HELPER TOOLS # e.g. d:\\OpenStaticAnalyzer-4.1.0-x64-Windows\\Java\\WindowsTools
j2cp_edition=THE NAME OF THE CHANGE PATH HELPER TOOL # e.g. JAN2ChangePath
```

## Acknowledgement
The development of the AIFix4SecCode framework was supported by the [AssureMOSS](https://assuremoss.eu) (Grant No.952647) EU-funded project.
