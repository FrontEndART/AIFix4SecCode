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
  readdirSync,
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
  ANALYZER_USE_DIFF_MODE,
  SetProjectFolder,
  utf8Stream,
} from "./constants";
import { IChange, IFix, Iissue, IProjectAnalysis } from "./interfaces";
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
import { applyPatchToFile } from "./patch";
import { getSafeFsPath } from "./path";
import { initActionCommands } from "./language/codeActions";
import * as cp from "child_process";
import upath from 'upath';

const parseJson = require("parse-json");
const parseDiff = require("parse-diff");
const diff = require("diff");
var path = require("path");

let activeDiffPanelWebviews = getActiveDiffPanelWebviews();

export let testView: TestView;

export function updateUserDecisions(
  decision: string,
  patchPath: string,
  leftPath: string
) {
  // Ask user's choice for accepting / declining the fix:
  logging.LogInfo("===== Executing showPopup command. =====");

  let inputOptions: vscode.InputBoxOptions = {
    prompt: "Please specify the reason for your choice: ",
    placeHolder: "I accepted / declined / reverted this fix because ...",
  };

  return vscode.window.showInputBox(inputOptions).then((value) => {
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
          path.join(patchRoot, "user_decisions.txt"), 
          `${dateStr} == ${leftPath} original File <-> ${patchPath} patch, decision: ${decision}, reason: ${value} \n`,
          utf8Stream
        );
    }
  });
}

export function init(
  context: vscode.ExtensionContext,
  jsonOutlineProvider: any
) {
  // Set working directory as PROJECT_FOLDER if no path was given in config:
  if (!PROJECT_FOLDER) {
    if (vscode.workspace.workspaceFolders! && vscode.workspace.workspaceFolders!.length > 0) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
      logging.LogInfoAndShowInformationMessage(
        "No project folder was given, setting opened workspace as project folder.",
        "No project folder was given, setting opened workspace as project folder."
      );
    } else {
      logging.LogErrorAndShowErrorMessage(
        "No workspace directory found! Open up a workspace directory or give a project path in config.",
        "No workspace directory found! Open up a workspace directory or give a project path in config."
      );
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
      "aifix4seccode-vscode.redoLastFix",
      redoLastFix
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
    logging.LogInfo(
      "===== Executing refreshAnalysisDiagnostics command. ====="
    );
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

  async function redoLastFix() {
    logging.LogInfo("===== Redo Last Fix started from command. =====");

    var lastFilePath = path.normalize(JSON.parse(
      context.workspaceState.get<string>("lastFilePath")!
    ));
    var lastFileContent = JSON.parse(
      context.workspaceState.get<string>("lastFileContent")!
    );
    var lastIssuesPath = path.normalize(JSON.parse(
      context.workspaceState.get<string>("lastIssuesPath")!
    ));
    var lastIssuesContent = JSON.parse(
      context.workspaceState.get<string>("lastIssuesContent")!
    );

    // Set content of issues:
    writeFileSync(lastIssuesPath, lastIssuesContent);

    // Set content of edited file and focus on it:
    writeFileSync(lastFilePath, lastFileContent);

    vscode.workspace.openTextDocument(lastFilePath).then((document) => {
      vscode.window.showTextDocument(document).then(() => {
        if (ANALYZER_USE_DIFF_MODE == "view Diffs") {
          var webview = getActiveDiffPanelWebview();
          if ("patchPath" in webview.params) {
            // Update user decisions of the revert fix:
            updateUserDecisions(
              "Undo was requested by user.",
              path.normalize(webview.params.patchPath!),
              lastFilePath
            );
          }
          getOutputFromAnalyzer();
        } else if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
          var patchFilepath = path.normalize(JSON.parse(
            context.workspaceState.get<string>("openedPatchPath")!
          ));

          // Update user decisions of the revert fix:
          updateUserDecisions(
            "Undo was requested by user.",
            patchFilepath,
            lastFilePath
          );
        }
      });
    });

    logging.LogInfo("===== Redo Last Fix command finished executing. =====");
  }

  function startAnalyzingProjectSync() {
    return new Promise<void>((resolve) => {
      if (!ANALYZER_EXE_PATH) {
        logging.LogErrorAndShowErrorMessage(
          "Unable to run analyzer! Analyzer executable path is missing.",
          "Unable to run analyzer! Analyzer executable path is missing."
        );
        resolve();
      } else if (!ANALYZER_PARAMETERS) {
        logging.LogErrorAndShowErrorMessage(
          "Unable to run analyzer! Analyzer parameters are missing.",
          "Unable to run analyzer! Analyzer parameters are missing."
        );
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
              logging.LogErrorAndShowErrorMessage(
                error.toString(),
                "Unable to run analyzer! " + error.toString()
              );
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
          logging.LogInfo(
            "issues got from analyzer output: " + JSON.stringify(output)
          );

          // Show issues treeView:
          // tslint:disable-next-line: no-unused-expression
          testView = new TestView(context);

          // Initialize action commands of diagnostics made after analysis:
          initActionCommands(context);

          resolve();
          logging.LogInfoAndShowInformationMessage(
            "===== Finished analysis. =====",
            "Finished analysis of project!"
          );

          process.exit();
        });
      }
    });
  }

  function openUpFile(patchPath: string) {
    logging.LogInfo("===== Executing openUpFile command. =====");
    let project_folder = PROJECT_FOLDER
    if (!PROJECT_FOLDER) {
      SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
    }

    var patch = "";
    try {
      logging.LogInfo("Reading patch from " + PATCH_FOLDER + "/" + patchPath);
      patch = readFileSync(path.join(PATCH_FOLDER, patchPath), "utf8");
    } catch (err) {
      logging.LogErrorAndShowErrorMessage(
        String(err),
        "Unable to read in patch file: " + err
      );
    }

    var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
    var sourceFile: string;
    if (sourceFileMatch && sourceFileMatch[1]) {
      sourceFile = sourceFileMatch[1];
    } else {
      logging.LogErrorAndShowErrorMessage(
        "Unable to find source file in '" + patchPath + "'",
        "Unable to find source file in '" + patchPath + "'"
      );
      throw Error("Unable to find source file in '" + patchPath + "'");
    }
    var openFilePath = vscode.Uri.file(upath.joinSafe(PROJECT_FOLDER, sourceFile));
    //var openFilePath = vscode.Uri.parse("file:///" + PROJECT_FOLDER + '/' + sourceFile); // not working on MacOS...

    logging.LogInfo("Running diagnosis in opened file...");
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
    logging.LogInfo("===== Finished openUpFile command. =====");
  }

  function loadPatch(patchPath: string) {
    logging.LogInfo("===== Executing loadPatch command. =====");

    // ==== LOAD PATCH IN "view Patch files" MODE: ====
    if (ANALYZER_USE_DIFF_MODE == "view Diffs") {
      if (!PROJECT_FOLDER) {
        SetProjectFolder(vscode.workspace.workspaceFolders![0].uri.path);
      }

      var patch = "";
      try {
        patch = readFileSync(path.join(PATCH_FOLDER, patchPath), "utf8");
      } catch (err) {
        logging.LogErrorAndShowErrorMessage(
          String(err),
          "Unable to read patch file: " + err
        );
      }

      var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
      var sourceFile: string;
      if (sourceFileMatch && sourceFileMatch[1]) {
        sourceFile = sourceFileMatch[1];
      } else {
        logging.LogErrorAndShowErrorMessage(
          "Unable to find source file in '" + patchPath + "'",
          "Unable to find source file in '" + patchPath + "'"
        );
        throw Error("Unable to find source file in '" + patchPath + "'");
      }
      var destinationFileMatch = /\+\+\+ ([^ \n\r\t]+).*/.exec(patch);
      var destinationFile;
      if (destinationFileMatch && destinationFileMatch[1]) {
        destinationFile = destinationFileMatch[1];
      } else {
        logging.LogErrorAndShowErrorMessage(
          "Unable to find destination file in '" + patchPath + "'",
          "Unable to find destination file in '" + patchPath + "'"
        );
        throw Error("Unable to find destination file in '" + patchPath + "'");
      }

      var original = readFileSync(path.join(PROJECT_FOLDER, sourceFile), "utf8");
      var patched = diff.applyPatch(original, patch);

      if(!patched){
        vscode.window.showErrorMessage('Failed to load patched version of this source file into a diff view! \n Make sure that your configuration is correct. Also make sure that the source file has not been patched already by this patch before! This issue may occour if the patch syntax is incorrect.'); 
        return;
      }

      if (isPatchAlreadyOpened(sourceFile)) {
        let requiredWebview = activeDiffPanelWebviews.find((webview) => {
          if ("leftPath" in webview.params) {
            if (webview.params.leftPath! === path.join(PROJECT_FOLDER, sourceFile)) {
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
        logging.LogErrorAndShowErrorMessage(
          "Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'",
          "Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'"
        );
        throw Error(
          "Failed to apply patch '" + patchPath + "' to '" + sourceFile + "'"
        );
      } else if (sourceFile !== destinationFile) {
        logging.LogInfo(
          "Applied '" +
            patchPath +
            "' to '" +
            sourceFile +
            "' and stored it as '" +
            destinationFile +
            "'"
        );
      } else {
        logging.LogInfo("Applied '" + patchPath + "' to '" + sourceFile + "'");
      }

      logging.LogInfo("Opening Diff view.");
      showDiff({
        patchPath: patchPath,
        leftContent: original,
        rightContent: patched,
        leftPath: path.join(PROJECT_FOLDER, sourceFile),
        rightPath: "",
        context,
        theme: vscode.window.activeColorTheme.kind.toString(),
      });
      vscode.commands.executeCommand("setContext", "patchApplyEnabled", true);
      // ==== LOAD PATCH IN "view Patch files" MODE: ====
    } else if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
      vscode.workspace
        .openTextDocument(path.join(PATCH_FOLDER, patchPath))
        .then((document) => {
          context.workspaceState.update(
            "openedPatchPath",
            JSON.stringify(path.join(PATCH_FOLDER, patchPath))
          );
          vscode.window.showTextDocument(document);
        });

      vscode.commands.executeCommand("setContext", "patchApplyEnabled", true);
    }
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

    if (ANALYZER_USE_DIFF_MODE == "view Diffs") {
      let patchPath = "";
      const webview = getActiveDiffPanelWebview();
      //let wasM = getPatchedContent(webview.params.leftContent, webview.params);

      // Saving issues.json and file contents in state,
      // so later the changes can be reverted if user asks for it:
      if ("leftPath" in webview.params) {
        saveFileAndFixesToState(webview.params.leftPath!);
      }

      webview.api.applyPatch();

      if ("leftPath" in webview.params && "patchPath" in webview.params) {
        var openFilePath = vscode.Uri.file(
          path.join(PROJECT_FOLDER, webview.params.leftPath)
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

      activeDiffPanelWebviews.splice(
        activeDiffPanelWebviews.indexOf(webview),
        1
      );
      if (activeDiffPanelWebviews.length < 1) {
        vscode.commands.executeCommand(
          "setContext",
          "patchApplyEnabled",
          false
        );
      }

      if ("patchPath" in webview.params && webview.params.patchPath) {
        patchPath = webview.params.patchPath;
      }
      testView.treeDataProvider?.refresh(patchPath);
    } else if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
      // 1. Get the content of the original file
      // 2. Apply the patch to it's content.
      // 3. Overwrite at the original file path with the patched content.
      // 4. Hide navbar buttons (applyPatch, declinePatch, nextDiff, prevDiff).

      // 1.
      var patchFilepath = JSON.parse(
        context.workspaceState.get<string>("openedPatchPath")!
      );
      var patchFileContent = readFileSync(path.normalize(patchFilepath), "utf8");
      var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patchFileContent);
      var sourceFile: string;
      if (sourceFileMatch && sourceFileMatch[1]) {
        sourceFile = sourceFileMatch[1];
      } else {
        throw Error("Unable to find source file in '" + patchFilepath + "'");
      }

      // Saving issues.json and file contents in state,
      // so later the changes can be reverted if user asks for it:
      saveFileAndFixesToState(path.normalize(path.join(PROJECT_FOLDER, sourceFile)));

      var sourceFileContent = readFileSync(
        path.normalize(path.join(PROJECT_FOLDER, sourceFile)),
        "utf8"
      ); 

      // 2.
      var destinationFileMatch = /\+\+\+ ([^ \n\r\t]+).*/.exec(
        patchFileContent
      );
      var destinationFile;
      if (destinationFileMatch && destinationFileMatch[1]) {
        destinationFile = destinationFileMatch[1];
      } else {
        throw Error(
          "Unable to find destination file in '" + patchFilepath + "'"
        );
      }
      var patched = diff.applyPatch(sourceFileContent, patchFileContent);

      console.log(patched);

      // 3.
      applyPatchToFile(
        path.normalize(path.join(PROJECT_FOLDER, sourceFile)),
        patched,
        patchFilepath
      );

      // 4.
      vscode.commands.executeCommand("setContext", "patchApplyEnabled", false);
    }
    logging.LogInfo("===== Finished applyPatch command. =====");
  }

  function declinePatch() {
    if (ANALYZER_USE_DIFF_MODE == "view Diffs") {
      let patchPath = "";
      const webview = getActiveDiffPanelWebview();
      activeDiffPanelWebviews.splice(
        activeDiffPanelWebviews.indexOf(webview),
        1
      );

      if ("leftPath" in webview.params && "patchPath" in webview.params) {
        updateUserDecisions(
          "declined",
          webview.params.patchPath!,
          webview.params.leftPath!
        );
        // var openFilePath = vscode.Uri.parse(
        //   "file:///" + PROJECT_FOLDER + "/" + webview.params.leftPath
        // );
        var openFilePath = path.normalize(path.join(PROJECT_FOLDER, webview.params.leftPath))
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
        vscode.commands.executeCommand(
          "setContext",
          "patchApplyEnabled",
          false
        );
      }

      if ("patchPath" in webview.params && webview.params.patchPath) {
        patchPath = webview.params.patchPath;
      }
      testView.treeDataProvider?.refresh(patchPath);

      webview.webViewPanel.dispose();
    } else if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
      // TODO: DO it with patch file
      let activeEditor = vscode.window.activeTextEditor!.document.uri.fsPath;
      var patchFilepath = JSON.parse(
        context.workspaceState.get<string>("openedPatchPath")!
      );
      var patchFileContent = readFileSync(patchFilepath, "utf8");
      var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patchFileContent);
      var sourceFile: string;
      if (sourceFileMatch && sourceFileMatch[1]) {
        sourceFile = sourceFileMatch[1];
      } else {
        throw Error("Unable to find source file in '" + patchFilepath + "'");
      }
      vscode.commands.executeCommand("setContext", "patchApplyEnabled", false);
      testView.treeDataProvider?.refresh(patchFilepath);
      vscode.workspace
        .openTextDocument(path.join(PROJECT_FOLDER, sourceFile))
        .then((document) => {
          vscode.window.showTextDocument(document).then(() => {
            vscode.window.withProgress(
              {
                location: vscode.ProgressLocation.Notification,
                title: "Loading Diagnostics...",
              },
              async () => {
                // 4.
                await refreshDiagnostics(
                  vscode.window.activeTextEditor!.document,
                  analysisDiagnostics
                );

                updateUserDecisions(
                  "declined",
                  patchFilepath,
                  path.join(PROJECT_FOLDER, sourceFile)
                );
              }
            );
          });
        });
    }
  }

  function saveFileAndFixesToState(filePath: string) {
    logging.LogInfo(filePath);

    let issuesPath: string | undefined = "";
    if (
      vscode.workspace
        .getConfiguration()
        .get<string>("aifix4seccode.analyzer.issuesPath")
    ) {
      issuesPath = vscode.workspace
        .getConfiguration()
        .get<string>("aifix4seccode.analyzer.issuesPath");
    }

    var originalFileContent = readFileSync(filePath!, "utf8");
    var originalIssuesContent = readFileSync(issuesPath!, "utf8");
    context.workspaceState.update(
      "lastFileContent",
      JSON.stringify(originalFileContent)
    );
    context.workspaceState.update("lastFilePath", JSON.stringify(filePath));

    context.workspaceState.update(
      "lastIssuesContent",
      JSON.stringify(originalIssuesContent)
    );
    context.workspaceState.update("lastIssuesPath", JSON.stringify(issuesPath));
    logging.LogInfo(filePath);
  }

  let currentFixId = 0;

  async function navigateDiff(step: number) {
    if (ANALYZER_USE_DIFF_MODE == "view Diffs") {
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
    } else if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
      var patchFilepath = JSON.parse(
        context.workspaceState.get<string>("openedPatchPath")!
      );
      var patchFileContent = readFileSync(patchFilepath, "utf8");
      var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patchFileContent);
      var sourceFile: string;
      if (sourceFileMatch && sourceFileMatch[1]) {
        sourceFile = sourceFileMatch[1];
      } else {
        throw Error("Unable to find source file in '" + patchFilepath + "'");
      }
      var leftPath = path.join(PROJECT_FOLDER, sourceFile);

      let fixes = await fakeAiFixCode.getFixes(leftPath);
      console.log(fixes);
      let nextFixId = currentFixId + step;
      if (!fixes[nextFixId]) {
        nextFixId = nextFixId > 0 ? 0 : fixes.length - 1;
      }

      vscode.workspace.openTextDocument(path.join(PATCH_FOLDER, fixes[nextFixId].path)).then((document) => {
        vscode.window.showTextDocument(document).then(() => {
          context.workspaceState.update(
            "openedPatchPath",
            JSON.stringify(path.join(PATCH_FOLDER, fixes[nextFixId].path))
          );
        });
      });
      currentFixId = nextFixId;
    }
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
