import { init } from "./commands";
//import { commands, ExtensionContext, languages, ProgressLocation, window, workspace, StatusBarItem, StatusBarAlignment } from 'vscode';
import * as vscode from "vscode";
import { log } from "./logger";
import { getVSCodeDownloadUrl } from "vscode-test/out/util";
import { JsonOutlineProvider } from "./providers/jsonOutline";
import { initActionCommands } from "./language/codeActions";
import * as logging from "./services/logging";
import * as constants from "./constants";
import { stringify } from "querystring";
var fs = require("fs");

var upath = require("upath");
var path = require("path");

export let analysisDiagnostics =
  vscode.languages.createDiagnosticCollection("aifix4seccode");

let analysisStatusBarItem: vscode.StatusBarItem;
let analyzeCurrentFileStatusBarItem: vscode.StatusBarItem;
let redoFixStatusBarItem: vscode.StatusBarItem;

export function activate(context: vscode.ExtensionContext) {
  const jsonOutlineProvider = new JsonOutlineProvider(context);
  vscode.window.registerTreeDataProvider("jsonOutline", jsonOutlineProvider);

  init(context, jsonOutlineProvider);
  log(process.env);
  saveConfigParameters();

  // status bar items:
  // Start analysis status bar item:
  analysisStatusBarItem = vscode.window.createStatusBarItem(
    vscode.StatusBarAlignment.Left,
    100
  );
  analysisStatusBarItem.command = "aifix4seccode-vscode.getOutputFromAnalyzer";
  context.subscriptions.push(analysisStatusBarItem);

  analysisStatusBarItem.text = "$(symbol-misc) Analyze Project";
  analysisStatusBarItem.show();

  // Analyze current file:
  analyzeCurrentFileStatusBarItem = vscode.window.createStatusBarItem(
    vscode.StatusBarAlignment.Left,
    100
  );
  analyzeCurrentFileStatusBarItem.command =
    "aifix4seccode-vscode.getOutputFromAnalyzerPerFile";
  context.subscriptions.push(analyzeCurrentFileStatusBarItem);

  analyzeCurrentFileStatusBarItem.text =
    "$(symbol-keyword) Analyze Current File";
  analyzeCurrentFileStatusBarItem.show();

  // Redo last fix:
  redoFixStatusBarItem = vscode.window.createStatusBarItem(
    vscode.StatusBarAlignment.Left,
    100
  );
  redoFixStatusBarItem.command = "aifix4seccode-vscode.redoLastFix";
  context.subscriptions.push(redoFixStatusBarItem);

  redoFixStatusBarItem.text = "$(redo) Undo Last Fix";
  redoFixStatusBarItem.show();

  // On settings change restart prompt:
  vscode.workspace.onDidChangeConfiguration((event) => {
    const action = "Reload";

    // save extension settings parameters to config file:
    saveConfigParameters();

    vscode.window
      .showInformationMessage(
        `Reload window in order for change in extension AIFix4SecCode configuration to take effect.`,
        action
      )
      .then((selectedAction) => {
        if (selectedAction === action) {
          vscode.commands.executeCommand("workbench.action.reloadWindow");
        }
      });
  });

  // Start up log:
  logging.LogInfo("Extension started!");
  vscode.window
    .showInformationMessage(
      "This extension is used for analyzing your project for issues. If you have no project folder opened please open it, or include it in the 'AIFix4SecCode' Extension settings.",
      "Open Settings"
    )
    .then((selected) => {
      vscode.commands.executeCommand(
        "workbench.action.openSettings",
        "AIFix4SecCode"
      );
    });
  logging.ShowInfoMessage("AIFix4SecCode installed. Welcome!");
}

function saveConfigParameters() {
  const config_path = constants.CONFIG_PATH;
  if (fs.existsSync(config_path)) {
    fs.readFile(config_path, "utf8", function (err: any, data: any) {
      if (err) {
        console.log(err);
      }

      // remove empty lines.
      data = data.split(/\r?\n/).filter((line:string) => line.trim() !== '').join('\n');

      var lines = data.split("\n");

      lines = lines
        .filter((line: string) => !line.startsWith("plugin") && !line.startsWith("# Vscode-Plugin"))
        .join("\n");

      fs.writeFileSync(config_path, lines);
      var logger = fs.createWriteStream(config_path, { flags: "a" });

      logger.write(constants.LOG_HEADING);

      logger.write(constants.ANALYZER_PARAMETERS_LOG);
      logger.write(constants.ANALYZER_EXE_PATH_LOG);
      logger.write(constants.PATCH_FOLDER_LOG);
      logger.write(constants.ISSUES_PATH_LOG);

      if (!constants.PROJECT_FOLDER || constants.PROJECT_FOLDER === "") {
        constants.SetProjectFolder(
          vscode.workspace.workspaceFolders![0].uri.path
        );
      }

      logger.write(constants.PROJECT_FOLDER_LOG);
      logger.write(constants.ANALYZER_USE_DIFF_MODE_LOG);
    });
  } else {
    var logger = fs.createWriteStream(config_path, { flags: "a" });

    logger.write(constants.LOG_HEADING);

    logger.write(constants.ANALYZER_PARAMETERS_LOG);
    logger.write(constants.ANALYZER_EXE_PATH_LOG);
    logger.write(constants.PATCH_FOLDER_LOG);
    logger.write(constants.ISSUES_PATH_LOG);

    if (!constants.PROJECT_FOLDER || constants.PROJECT_FOLDER === "") {
      constants.SetProjectFolder(
        vscode.workspace.workspaceFolders![0].uri.path
      );
    }

    logger.write(constants.PROJECT_FOLDER_LOG);
    logger.write(constants.ANALYZER_USE_DIFF_MODE_LOG);

    logging.LogInfoAndShowInformationMessage(
      "config.properties file was not found at the given location so the plugin created one by default. Please note that only the plugin's configuration was added to the file and in order to successfully run the analyzer executable, you need to add additional configuration parameters in to the file! You can read more about this at https://github.com/FrontEndART/AIFix4SecCode",
      "config.properties file was not found at the given location so the plugin created one by default. Please note that only the plugin's configuration was added to the file and in order to successfully run the analyzer executable, you need to add additional configuration parameters in to the file! You can read more about this at https://github.com/FrontEndART/AIFix4SecCode"
    )
  }
}
