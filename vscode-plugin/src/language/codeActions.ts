import { readFileSync } from 'fs';
import * as vscode from 'vscode';
import { PATCH_FOLDER, PROJECT_FOLDER } from '../constants';
import { IFix, Iissue, IIssueRange } from '../interfaces';
import { getIssues } from '../services/fakeAiFixCode';
import { getSafeFsPath, getSafePath } from "../path";

var path = require('path');
var upath = require('upath');

let issues: Iissue | undefined;
let disposableAnalyzerProvider : vscode.Disposable;
let disposableAnalyzerInfoProvider : vscode.Disposable;

async function initIssues() {
    issues = await getIssues();
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
        if(disposableAnalyzerProvider){
            disposableAnalyzerProvider.dispose()
        }

        if(disposableAnalyzerInfoProvider){
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

    public provideCodeActions(document: vscode.TextDocument, range: vscode.Range, context: vscode.CodeActionContext): vscode.CodeAction[] | undefined {

        let commandActions: vscode.CodeAction[] = [];
        
        if (issues) {
            Object.values(issues).forEach(issue => {
                const fixRange = issue.textRange;
                issue.patches.forEach((fix: IFix) => {
                    const fixText = fix.explanation;
                    const patchPath = fix.path;
                    var patch = '';

                    try {
                        patch = readFileSync(upath.joinSafe(PATCH_FOLDER, patchPath), "utf8");
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

                    let openedFilePath = vscode.window.activeTextEditor?.document.uri.fsPath;
                    
                    let codeActionTargetPath = upath.normalize(upath.joinSafe(PROJECT_FOLDER, sourceFilePath)).toLowerCase()
                    let currentlyOpenedFilePath = upath.normalize(openedFilePath).toLowerCase()
                    
                    codeActionTargetPath = getSafePath(codeActionTargetPath)
                    currentlyOpenedFilePath = getSafePath(currentlyOpenedFilePath)
                    
                    if(codeActionTargetPath === currentlyOpenedFilePath){
                        let foundMatchingDiagnostic = context.diagnostics.some((diagnostic) => 
                            diagnostic.range.start.line === fixRange.startLine - 1 && diagnostic.range.end.line === fixRange.endLine - 1
                        )
                        if(foundMatchingDiagnostic)
                            commandActions.push(this.createCommand(fixText, fixRange, patchPath));
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