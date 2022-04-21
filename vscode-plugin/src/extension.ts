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
let redoFixStatusBarItem : vscode.StatusBarItem;

export function activate(context: vscode.ExtensionContext) {
  const jsonOutlineProvider = new JsonOutlineProvider(context);
	vscode.window.registerTreeDataProvider('jsonOutline', jsonOutlineProvider);
  
  init(context, jsonOutlineProvider);
  log(process.env);

  // status bar items:
  // Start analysis status bar item:
  analysisStatusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
  analysisStatusBarItem.command = 'aifix4seccode-vscode.getOutputFromAnalyzer';
  context.subscriptions.push(analysisStatusBarItem);
  
  analysisStatusBarItem.text = "$(symbol-misc) Start Analysis";
  analysisStatusBarItem.show();

  // Redo last fix:
  redoFixStatusBarItem = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Left, 100);
  redoFixStatusBarItem.command = 'aifix4seccode-vscode.redoLastFix';
  context.subscriptions.push(redoFixStatusBarItem);
  
  redoFixStatusBarItem.text = "$(redo) Undo Last Fix";
  redoFixStatusBarItem.show();
  
  // On settings change restart prompt:
  vscode.workspace.onDidChangeConfiguration(event => {
    const action = 'Reload';

    vscode.window
      .showInformationMessage(
        `Reload window in order for change in extension AIFix4SecCode configuration to take effect.`,
        action
      )
      .then(selectedAction => {
        if (selectedAction === action) {
          vscode.commands.executeCommand('workbench.action.reloadWindow');
        }
      });
  })

  // Start up log:
  logging.LogInfo("Extension started!");
  vscode.window.showInformationMessage('This extension is used for analyzing your project for issues. If you have no project folder opened please open it, or include it in the \'AIFix4SecCode\' Extension settings.'
  , 'Open Settings').then(selected => {
    vscode.commands.executeCommand('workbench.action.openSettings', 'AIFix4SecCode');
  });
  logging.ShowInfoMessage("AIFix4SecCode installed. Welcome!");
}