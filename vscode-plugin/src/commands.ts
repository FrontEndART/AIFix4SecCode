import { showDiff, showNotSupported } from "./webview";
import {
  readFileSync,
  writeFileSync,
  existsSync,
  fstat,
  access,
  watch,
  constants,
  appendFileSync,
} from "fs";
import * as vscode from "vscode";
import {
  getActiveDiffPanelWebview,
  getActiveDiffPanelWebviews,
} from "./webview/store";
import {
  ANALYZER_EXE_PATH,
  PATCH_FOLDER,
  PROJECT_FOLDER,
  ANALYZER_PARAMETERS,
  SetProjectFolder,
  utf8Stream,
} from "./constants";
import { IChange, IProjectAnalysis } from "./interfaces";
import {
  ExtendedWebview,
  ExtendedWebviewEnv,
  IExtendedWebviewEnvDiff,
} from "./webview/extendedWebview";
import { refreshDiagnostics } from "./language/diagnostics";
import { TestView } from "./providers/testView";
import { analysisDiagnostics } from "./extension";
import * as fakeAiFixCode from "./services/fakeAiFixCode";
import * as logging from "./services/logging";
import { SymbolDisplayPartKind, WatchDirectoryFlags } from "typescript";
import { basename, dirname } from "path";
import { getSafeFsPath } from "./path";
import { initActionCommands } from "./language/codeActions";
import * as cp from "child_process";

const parseJson = require("parse-json");
const parseDiff = require("parse-diff");
const diff = require("diff");
var path = require("path");

let activeDiffPanelWebviews = getActiveDiffPanelWebviews();

export let testView : TestView;

export function updateUserDecisions(
  decision: string,
  patchPath: string,
  leftPath: string
) {
  let patchRoot = PATCH_FOLDER;
  if (patchRoot) {
    let date = new Date();
    let dateStr =
      date.getFullYear().toString() +
      "/" +
      (date.getMonth() + 1).toString() +
      "/" +
      date.getDate().toString() +
      " " +
      date.getHours().toString() +
      ":" +
      date.getMinutes().toString();

    appendFileSync(
      getSafeFsPath(path.join(patchRoot, "user_decisions.txt")),
      dateStr +
        " == " +
        leftPath +
        " original File <-> " +
        patchPath +
        " patch" +
        ", decision: " +
        decision +
        "\n",
      utf8Stream
    );
  }
}

export function init(
  context: vscode.ExtensionContext,
  jsonOutlineProvider: any
) {
  // Set working directory as PROJECT_FOLDER if no path was given in config:
  if (!PROJECT_FOLDER) {
    if (vscode.workspace.workspaceFolders!.length > 0) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
      logging.LogInfoAndShowInformationMessage("No project folder was given, setting opened workspace as project folder.", "No project folder was given, setting opened workspace as project folder.");
    } else {
      logging.LogErrorAndShowErrorMessage("No workspace directory found! Open up a workspace directory or give a project path in config.", "No workspace directory found! Open up a workspace directory or give a project path in config.");
    }

    // Filter out first backslash from project path:
    let projectFolder = PROJECT_FOLDER;
    if (projectFolder!.startsWith("/"))
      projectFolder = projectFolder!.replace("/", "");

    SetProjectFolder(projectFolder!);
  }

  context.subscriptions.push(
    vscode.commands.registerCommand("aifix4seccode-vscode.blank", blank),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.loadPatchFile",
      loadPatch
    ),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.applyPatch",
      applyPatch
    ),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.declinePatch",
      declinePatch
    ),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.refreshDiagnostics",
      refreshAnalysisDiagnostics
    ),
    vscode.commands.registerCommand("aifix4seccode-vscode.nextDiff", nextDiff),
    vscode.commands.registerCommand("aifix4seccode-vscode.prevDiff", prevDiff),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.getOutputFromAnalyzer",
      getOutputFromAnalyzer
    ),
    vscode.commands.registerCommand(
      "aifix4seccode-vscode.openUpFile",
      openUpFile
    ),
    // treeview
    vscode.commands.registerCommand("jsonOutline.refresh", () =>
      jsonOutlineProvider.refresh()
    ),
    vscode.commands.registerCommand("jsonOutline.refreshNode", (offset) =>
      jsonOutlineProvider.refresh(offset)
    ),
    vscode.commands.registerCommand("jsonOutline.renameNode", (offset) =>
      jsonOutlineProvider.rename(offset)
    ),
    vscode.commands.registerCommand("extension.openJsonSelection", (range) =>
      jsonOutlineProvider.select(range)
    )
  );

  vscode.commands.executeCommand("setContext", "patchApplyEnabled", false);

  function blank() {
    showDiff({ leftContent: "", rightContent: "", rightPath: "", context });
    vscode.commands.executeCommand("setContext", "patchApplyEnabled", true);
  }

  async function refreshAnalysisDiagnostics() {
    logging.LogInfo("===== Executing refreshAnalysisDiagnostics command. =====")
    vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: "Analyzing Project...",
      },
      async () => {
        await refreshDiagnostics(
          vscode.window.activeTextEditor!.document,
          analysisDiagnostics
        );
      }
    );
  }

  async function getOutputFromAnalyzer() {
    logging.LogInfo("===== Analysis started from command. =====");
    vscode.window.withProgress(
      {
        location: vscode.ProgressLocation.Notification,
        title: "Analyzing project!",
        cancellable: false,
      },
      async () => {
        return startAnalyzingProjectSync();
      }
    );
  }

  function startAnalyzingProjectSync() {
    return new Promise((resolve) => {
      if (!ANALYZER_EXE_PATH) {
        logging.LogErrorAndShowErrorMessage("Unable to run analyzer! Analyzer executable path is missing.", "Unable to run analyzer! Analyzer executable path is missing.");
        resolve();
      } else if (!ANALYZER_PARAMETERS) {
        logging.LogErrorAndShowErrorMessage("Unable to run analyzer! Analyzer parameters are missing.", "Unable to run analyzer! Analyzer parameters are missing.");
        resolve();
      } else {
        // run analyzer with terminal (read params and analyzer path from config):
        logging.LogInfo("Analyzer executable started.");
        logging.LogInfo("Running " + ANALYZER_PARAMETERS);

        var child = cp.exec(
          ANALYZER_PARAMETERS,
          { cwd: ANALYZER_EXE_PATH },
          (error) => {
            if (error) {
              logging.LogErrorAndShowErrorMessage(error.toString(), "Unable to run analyzer! " + error.toString());
            }
          }
        );
        child.stdout.pipe(process.stdout);
        // waiting for analyzer to finish, only then read the output.
        child.on("exit", function () {
          // if executable has finished:
          logging.LogInfo("Analyzer executable finished.");
          // Get Output from analyzer:
          let output = fakeAiFixCode.getIssuesSync();
          logging.LogInfo("issues got from analyzer output: " + JSON.stringify(output));
          
          // Show issues treeView:
          // tslint:disable-next-line: no-unused-expression
          testView = new TestView(context);

          let issuesFilePath = vscode.workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesFilePath')
          

          resolve();
          logging.LogInfoAndShowInformationMessage("===== Finished analysis. =====", "Finished analysis of project!");

          process.exit();
        });
      }
    });
  }

  function openUpFile(patchPath: string) {
    logging.LogInfo("===== Executing openUpFile command. =====");

    if (!PROJECT_FOLDER) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
    }

    var patch = "";
    try {
      logging.LogInfo("Reading patch from " + PATCH_FOLDER + "/" + patchPath)
      patch = readFileSync(PATCH_FOLDER + "/" + patchPath, "utf8");
    } catch (err) {
      logging.LogErrorAndShowErrorMessage(err, "Unable to read in patch file: " + err);
    }

    var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
    var sourceFile: string;
    if (sourceFileMatch && sourceFileMatch[1]) {
      sourceFile = sourceFileMatch[1];
    } else {
      logging.LogErrorAndShowErrorMessage("Unable to find source file in '" + patchPath + "'", "Unable to find source file in '" + patchPath + "'");
      throw Error("Unable to find source file in '" + patchPath + "'");
    }
    let projectFolder = PROJECT_FOLDER
    var openFilePath = vscode.Uri.file(PROJECT_FOLDER + "/" + sourceFile);
    //var openFilePath = vscode.Uri.parse("file:///" + PROJECT_FOLDER + '/' + sourceFile); // not working on MacOS...
    
    logging.LogInfo("Running diagnosis in opened file...")
    vscode.workspace.openTextDocument(openFilePath).then((document) => {
      // ==== showTextDocument should trigger providecodeActions function, if not something is wrong with the path... ====
      vscode.window.showTextDocument(document).then(() => { 
        vscode.window.withProgress(
          {
            location: vscode.ProgressLocation.Notification,
            title: "Loading Diagnostics...",
          },
          async () => {
            await refreshDiagnostics(
              vscode.window.activeTextEditor!.document,
              analysisDiagnostics
            );
          }
        );
      });
    });
    logging.LogInfo("===== Finished openUpFile command. =====");
  }

  function loadPatch(patchPath: string) {
    logging.LogInfo("===== Executing loadPatch command. =====");
    if (!PROJECT_FOLDER) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
    }

    var patch = "";
    try {
      patch = readFileSync(PATCH_FOLDER + "/" + patchPath, "utf8");
    } catch (err) {
      logging.LogErrorAndShowErrorMessage(err, "Unable to read patch file: " + err);
    }

    var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
    var sourceFile: string;
    if (sourceFileMatch && sourceFileMatch[1]) {
      sourceFile = sourceFileMatch[1];
    } else {
      logging.LogErrorAndShowErrorMessage("Unable to find source file in '" + patchPath + "'", "Unable to find source file in '" + patchPath + "'");
      throw Error("Unable to find source file in '" + patchPath + "'");
    }
    var destinationFileMatch = /\+\+\+ ([^ \n\r\t]+).*/.exec(patch);
    var destinationFile;
    if (destinationFileMatch && destinationFileMatch[1]) {
      destinationFile = destinationFileMatch[1];
    } else {
      logging.LogErrorAndShowErrorMessage("Unable to find destination file in '" + patchPath + "'", "Unable to find destination file in '" + patchPath + "'");
      throw Error("Unable to find destination file in '" + patchPath + "'");
    }

    var original = readFileSync(PROJECT_FOLDER + "/" + sourceFile, "utf8");
    var patched = diff.applyPatch(original, patch);
    if (isPatchAlreadyOpened(sourceFile)) {
      let requiredWebview = activeDiffPanelWebviews.find((webview) => {
        if ("leftPath" in webview.params) {
          if (webview.params.leftPath! === PATCH_FOLDER + "/" + sourceFile) {
            return webview;
          }
        }
      });

      if (requiredWebview) {
        requiredWebview!.webViewPanel.reveal(vscode.ViewColumn.One, false);
      }
      return;
    }

    if (patched === false) {
      logging.LogErrorAndShowErrorMessage("Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'", "Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'");
      throw Error(
        "Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'"
      );
    } else if (sourceFile !== destinationFile) {
      logging.LogInfo("Applied '" + patchPath + "' to '" + sourceFile + "' and stored it as '" + destinationFile + "'");
    } else {
      logging.LogInfo("Applied '" + patchPath + "' to '" + sourceFile + "'");
    }

    logging.LogInfo("Opening Diff view.");
    showDiff({
      patchPath: patchPath,
      leftContent: original,
      rightContent: patched,
      leftPath: PATCH_FOLDER + "/" + sourceFile,
      rightPath: "",
      context,
      theme: vscode.window.activeColorTheme.kind.toString(),
    });
    vscode.commands.executeCommand("setContext", "patchApplyEnabled", true);
    logging.LogInfo("===== Finished loadPatch command. =====");
  }

  function getPatchedContent(original: string, params: any) {
    if (!PROJECT_FOLDER) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
    }

    var patch = "";
    try {
      patch = readFileSync(PATCH_FOLDER + "/" + params.patchPath, "utf8");
    } catch (err) {
      console.log(err);
    }

    var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
    var sourceFile: string;
    if (sourceFileMatch && sourceFileMatch[1]) {
      sourceFile = sourceFileMatch[1];
    } else {
      throw Error("Unable to find source file in '" + params.patchPath + "'");
    }
    var destinationFileMatch = /\+\+\+ ([^ \n\r\t]+).*/.exec(patch);
    var destinationFile;
    if (destinationFileMatch && destinationFileMatch[1]) {
      destinationFile = destinationFileMatch[1];
    } else {
      throw Error(
        "Unable to find destination file in '" + params.patchPath + "'"
      );
    }
    var patched = diff.applyPatch(original, patch);
    return patched;
  }

  function applyPatch() {
    logging.LogInfo("===== Executing applyPatch command. =====");
    let patchPath = "";
    const webview = getActiveDiffPanelWebview();
    //let wasM = getPatchedContent(webview.params.leftContent, webview.params);
    webview.api.applyPatch();

    if ("leftPath" in webview.params && "patchPath" in webview.params) {
      //var openFilePath = vscode.Uri.parse("file:///" + PROJECT_FOLDER + '/' + webview.params.leftPath); // not working on MacOS...
      var openFilePath = vscode.Uri.file(
        PROJECT_FOLDER + "/" + webview.params.leftPath
      );
      vscode.workspace.openTextDocument(openFilePath).then((document) => {
        vscode.window.showTextDocument(document).then(() => {
          vscode.window.withProgress(
            {
              location: vscode.ProgressLocation.Notification,
              title: "Loading Diagnostics...",
            },
            async () => {
              await refreshDiagnostics(
                vscode.window.activeTextEditor!.document,
                analysisDiagnostics
              );
            }
          );
        });
      });
    }

    activeDiffPanelWebviews.splice(activeDiffPanelWebviews.indexOf(webview), 1);
    if (activeDiffPanelWebviews.length < 1) {
      vscode.commands.executeCommand("setContext", "patchApplyEnabled", false);
    }

    if ("patchPath" in webview.params && webview.params.patchPath) {
      patchPath = webview.params.patchPath;
    }
    testView.treeDataProvider?.refresh(patchPath);
    logging.LogInfo("===== Finished applyPatch command. =====");
  }

  function declinePatch() {
    let patchPath = "";
    const webview = getActiveDiffPanelWebview();
    activeDiffPanelWebviews.splice(activeDiffPanelWebviews.indexOf(webview), 1);

    if ("leftPath" in webview.params && "patchPath" in webview.params) {
      updateUserDecisions(
        "declined",
        webview.params.patchPath!,
        webview.params.leftPath!
      );
      var openFilePath = vscode.Uri.parse(
        "file:///" + PROJECT_FOLDER + "/" + webview.params.leftPath
      );
      vscode.workspace.openTextDocument(openFilePath).then((document) => {
        vscode.window.showTextDocument(document).then(() => {
          vscode.window.withProgress(
            {
              location: vscode.ProgressLocation.Notification,
              title: "Loading Diagnostics...",
            },
            async () => {
              await refreshDiagnostics(
                vscode.window.activeTextEditor!.document,
                analysisDiagnostics
              );
            }
          );
        });
      });
    }

    if (activeDiffPanelWebviews.length < 1) {
      vscode.commands.executeCommand("setContext", "patchApplyEnabled", false);
    }

    if ("patchPath" in webview.params && webview.params.patchPath) {
      patchPath = webview.params.patchPath;
    }
    testView.treeDataProvider?.refresh(patchPath);

    webview.webViewPanel.dispose();
  }

  let currentFixId = 0;

  async function navigateDiff(step: number) {
    let activeWebview = getActiveDiffPanelWebview();
    let origPath = "";
    if ("leftPath" in activeWebview.params) {
      origPath = activeWebview.params.leftPath!;
    }

    let fixes = await fakeAiFixCode.getFixes(origPath);
    let nextFixId = currentFixId + step;
    if (!fixes[nextFixId]) {
      nextFixId = nextFixId > 0 ? 0 : fixes.length - 1;
    }

    let sourceFile = "";
    let requiredWebview = activeDiffPanelWebviews.find((webview) => {
      if ("patchPath" in webview.params) {
        if (webview.params.patchPath! === fixes[nextFixId].path) {
          return webview;
        }
      }
    });

    if (requiredWebview) {
      requiredWebview!.webViewPanel.reveal(vscode.ViewColumn.One, false);
    } else {
      let leftContent = getLeftContent(fixes[nextFixId].path);
      let rightContent = getRightContent(fixes[nextFixId].path, leftContent);
      showDiff({
        patchPath: fixes[nextFixId].path,
        leftContent: leftContent,
        rightContent: rightContent,
        leftPath: origPath,
        rightPath: "",
        context,
      });
    }
    currentFixId = nextFixId;
  }

  function nextDiff() {
    logging.LogInfo("===== Executing nextDiff command. =====");
    navigateDiff(+1);
    logging.LogInfo("===== Finished nextDiff command. =====");
  }

  function prevDiff() {
    logging.LogInfo("===== Executing nextDiff command. =====");
    navigateDiff(-1);
    logging.LogInfo("===== Finished nextDiff command. =====");
  }

  function isPatchAlreadyOpened(sourceFile: string) {
    return activeDiffPanelWebviews.some((x: ExtendedWebview) => {
      // compiler will not accept x.params.leftPath as a valid property of its own type (ExtendedWebviewEnv), because it is a inherited
      // property. That is the reason we use this if statement.
      if ("leftPath" in x.params) {
        return (
          x.params.leftPath!.substring(
            x.params.leftPath!.lastIndexOf("/") + 1,
            x.params.leftPath!.length
          ) === sourceFile
        );
      }
    });
  }

  function getLeftContent(patchPath: string) {
    if (!PROJECT_FOLDER) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
    }

    var outputFolder = vscode.workspace
      .getConfiguration()
      .get<string>("aifix4seccode.analyzer.generatedPatchesPath");
    if (!outputFolder) {
      outputFolder = vscode.workspace.workspaceFolders![0].uri.path;
    }

    var patch = "";
    try {
      patch = readFileSync(outputFolder + "/" + patchPath, "utf8");
    } catch (err) {
      console.log(err);
    }
    var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
    var sourceFile: string;
    if (sourceFileMatch && sourceFileMatch[1]) {
      sourceFile = sourceFileMatch[1];
    } else {
      throw Error("Unable to find source file in '" + patchPath + "'");
    }

    var original = readFileSync(PROJECT_FOLDER + "/" + sourceFile, "utf8");
    return original;
  }

  function getRightContent(patchPath: string, original: string) {
    var outputFolder = vscode.workspace
      .getConfiguration()
      .get<string>("aifix4seccode.analyzer.generatedPatchesPath");
    if (!outputFolder) {
      outputFolder = vscode.workspace.workspaceFolders![0].uri.path;
    }

    var patch = "";
    try {
      patch = readFileSync(outputFolder + "/" + patchPath, "utf8");
    } catch (err) {
      console.log(err);
    }
    var destinationFileMatch = /\+\+\+ ([^ \n\r\t]+).*/.exec(patch);
    var destinationFile;
    if (destinationFileMatch && destinationFileMatch[1]) {
      destinationFile = destinationFileMatch[1];
    } else {
      throw Error("Unable to find destination file in '" + patchPath + "'");
    }
    var patched = diff.applyPatch(original, patch);

    return patched;
  }
}
