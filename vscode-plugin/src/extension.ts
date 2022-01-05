import { init } from './commands';
//import { commands, ExtensionContext, languages, ProgressLocation, window, workspace, StatusBarItem, StatusBarAlignment } from 'vscode';
import * as vscode from 'vscode';
import { log } from './logger';
import { getVSCodeDownloadUrl } from 'vscode-test/out/util';
import { JsonOutlineProvider } from './providers/jsonOutline';
import { initActionCommands } from './language/codeActions';
import * as logging from './services/logging';

export let analysisDiagnostics = vscode.languages.createDiagnosticCollection('aifix4seccode');

let analysisStatusBarItem : vscode.StatusBarItem;

export function activate(context: vscode.ExtensionContext) {
  const jsonOutlineProvider = new JsonOutlineProvider(context);
	vscode.window.registerTreeDataProvider('jsonOutline', jsonOutlineProvider);
  
  init(context, jsonOutlineProvider);
  // Initialize action commands of diagnostics made after analysis:
  initActionCommands(context);
  log(process.env);

  // status bar item
  analysisStatusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
  analysisStatusBarItem.command = 'aifix4seccode-vscode.getOutputFromAnalyzer';
  context.subscriptions.push(analysisStatusBarItem);
  
  analysisStatusBarItem.text = "$(symbol-misc) Start Analysis"
  analysisStatusBarItem.show()
  
  // Start up log:
  logging.LogInfo("Extension started!");
  vscode.window.showInformationMessage('This extension is used for analyzing your project for issues. If you have no project folder opened please open it, or include it in the DiffMerge Extension settings.'
  , 'Open Settings').then(selected => {
    vscode.commands.executeCommand('workbench.action.openSettings', 'AIFix4SecCode');
  });
  logging.ShowInfoMessage("AIFix4SecCode installed. Welcome!");
}