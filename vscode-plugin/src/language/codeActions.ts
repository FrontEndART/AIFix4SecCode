import { readFileSync } from 'fs';
import * as vscode from 'vscode';
import { PATCH_FOLDER, PROJECT_FOLDER } from '../constants';
import { IFix, Iissue, IIssueRange } from '../interfaces';
import { getIssues } from '../services/fakeAiFixCode';
var path = require('path');
var upath = require('upath');

let issueGroups = {};
let disposableAnalyzerProvider: vscode.Disposable;
let disposableAnalyzerInfoProvider: vscode.Disposable;

async function initIssues() {
  issueGroups = await getIssues();
  console.log(issueGroups);
}

export function initActionCommands(context: vscode.ExtensionContext) {
  initIssues().then(() => {
    // java, c#, c
    // WIP

    // if we only want to apply actions to only typescript files.
    let typescriptSelector = {
      scheme: 'file',
      language: 'typescript'
    };

    // if we only want to apply actions to only java files.
    let javaSelector = {
      scheme: 'file',
      language: 'java'
    };


    // Dispose already created providers to avoid duplication issues:
    if (disposableAnalyzerProvider) {
      disposableAnalyzerProvider.dispose()
    }

    if (disposableAnalyzerInfoProvider) {
      disposableAnalyzerInfoProvider.dispose()
    }


    // use "*" as DocumentSelector argument otherwise...
    context.subscriptions.push(
      disposableAnalyzerProvider = vscode.languages.registerCodeActionsProvider("*", new Analyzer(), {
        providedCodeActionKinds: Analyzer.providedCodeActionKinds
      }));

    context.subscriptions.push(
      disposableAnalyzerInfoProvider = vscode.languages.registerCodeActionsProvider("*", new AnalyzerInfo(), {
        providedCodeActionKinds: AnalyzerInfo.providedCodeActionKinds
      })
    );
  });
}

export class Analyzer implements vscode.CodeActionProvider {

  public static readonly providedCodeActionKinds = [
    vscode.CodeActionKind.QuickFix
  ];
  // called whenever the user selects text or places the cursor in an area that contains a Diagnostic:
  public async provideCodeActions(document: vscode.TextDocument, range: vscode.Range): Promise<vscode.CodeAction[] | undefined> {

    let commandActions: vscode.CodeAction[] = [];
    issueGroups = await getIssues();
    if (issueGroups) {
      Object.values(issueGroups).forEach((issues: any) => {
        issues.forEach((issue: any) => {
          if (issue.textRange.startLine - 1 === range.start.line) {
            const fixRange = issues.textRange;
            issue.patches.sort((a: any, b: any) => b.score - a.score);
            issue.patches.forEach((fix: IFix) => {
              const fixText = fix.explanation;
              const patchPath = fix.path;
              var patch = '';
              var patch_folder = PATCH_FOLDER;

              if (process.platform === 'win32') {
                if (patch_folder[0] === '/' || patch_folder[0] === '\\') {
                  patch_folder = patch_folder.substring(1);
                }
              }

              try {
                patch = readFileSync(upath.join(patch_folder, patchPath), "utf8");
              } catch (err) {
                console.log(err);
              }

              var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
              var sourceFilePath: string;

              if (sourceFileMatch && sourceFileMatch[1]) {
                sourceFilePath = sourceFileMatch[1];
              } else {
                throw Error("Unable to find source file in '" + patch + "'");
              }

              let openedFilePath = vscode.window.activeTextEditor?.document.uri.path;

              let projectFolder = PROJECT_FOLDER

              sourceFilePath = upath.normalize(upath.join(PROJECT_FOLDER, vscode.Uri.file(sourceFilePath).fsPath).toLowerCase())
              openedFilePath = upath.normalize(vscode.Uri.file(openedFilePath!).fsPath.toLowerCase())
              if (process.platform === 'linux' || process.platform === 'darwin') {
                if (sourceFilePath![0] !== '/')
                  sourceFilePath = '/' + sourceFilePath
                if (openedFilePath![0] !== '/')
                  openedFilePath = '/' + openedFilePath
              }

              if (process.platform === 'win32') {
                if (sourceFilePath[0] === '/' || sourceFilePath[0] === '\\') {
                  sourceFilePath = sourceFilePath.substring(1);
                }
              }

              let editor = vscode.window.activeTextEditor;
              let cursorPosition = editor?.selection.start;
              if (cursorPosition) {
                if (sourceFilePath === openedFilePath && cursorPosition!.line === range.start.line) {
                  commandActions.push(this.createCommand(fixText, fixRange, patchPath));
                }
              }
            });
          }
        });
      });
    }

    return commandActions;
  }

  private createCommand(fixText: string, fixRange: IIssueRange, patchPath: string): vscode.CodeAction {
    const action = new vscode.CodeAction(fixText, vscode.CodeActionKind.QuickFix);
    action.command = { command: 'aifix4seccode-vscode.loadPatchFile', arguments: [patchPath], title: 'Refactor code with possible fixes.', tooltip: 'this will open a diff view with the generated patch.' };
    return action;
  }
}

export class AnalyzerInfo implements vscode.CodeActionProvider {

  public static readonly providedCodeActionKinds = [
    vscode.CodeActionKind.QuickFix,
  ];

  provideCodeActions(document: vscode.TextDocument, range: vscode.Range | vscode.Selection, context: vscode.CodeActionContext, token: vscode.CancellationToken): vscode.CodeAction[] {
    // for each diagnostic entry that has the matching `code`, create a code action command
    return [];
  }
}