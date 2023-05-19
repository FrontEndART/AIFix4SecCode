import { readFileSync } from "fs";
import * as vscode from "vscode";
import { ANALYZER_MENTION, PATCH_FOLDER, PROJECT_FOLDER } from "../constants";
import { IFix, Iissue, IIssueRange } from "../interfaces";
import { getIssues } from "../services/fakeAiFixCode";
import * as logging from "../services/logging";
var path = require("path");
var upath = require("upath");

let issueGroups = {};

async function initIssues() {
  issueGroups = await getIssues();
}

export async function refreshDiagnostics(
  doc: vscode.TextDocument,
  aiFixCodeDiagnostics: vscode.DiagnosticCollection
) {
  try {
    const diagnostics: vscode.Diagnostic[] = [];
    initIssues().then(() => {
      // for each issue we create a diagnosctic.
      if (issueGroups) {
        Object.values(issueGroups).forEach((issues: any) => {
          issues.forEach((issue: any) => {
            const fixRange = issue.textRange;
            issue.patches.forEach((fix: IFix) => {
              const fixText = fix.explanation;
              const patchPath = fix.path;
              var patch = "";
              var patch_folder = PATCH_FOLDER;

              if (process.platform === 'win32') {
                if (patch_folder[0] === '/' || patch_folder[0] === '\\') {
                  patch_folder = patch_folder.substring(1);
                }
              }

              try {
                patch = readFileSync(upath.join(patch_folder, patchPath), "utf8");
              } catch (err) {
                logging.LogErrorAndShowErrorMessage(
                  "Error with readFileSync patch file: " + err,
                  "Cannot refresh diagnostics! There was a problem with patch file: " +
                  err
                );
              }

              var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
              var sourceFilePath: string;

              if (sourceFileMatch && sourceFileMatch[1]) {
                sourceFilePath = sourceFileMatch[1];
              } else {
                logging.LogErrorAndShowErrorMessage(
                  "Unable to find source file in '" + patch + "'",
                  "Unable to find source file in '" + patch + "'"
                );
                throw Error("Unable to find source file in '" + patch + "'");
              }

              let openedFilePath =
                vscode.window.activeTextEditor?.document.uri.path;
              let projectFolder = PROJECT_FOLDER;

              sourceFilePath = upath.normalize(
                upath
                  .join(PROJECT_FOLDER, vscode.Uri.file(sourceFilePath).fsPath)
                  .toLowerCase()
              );
              openedFilePath = upath.normalize(
                vscode.Uri.file(openedFilePath!).fsPath.toLowerCase()
              );
              if (
                process.platform === "linux" ||
                process.platform === "darwin"
              ) {
                if (sourceFilePath![0] !== "/")
                  sourceFilePath = "/" + sourceFilePath;
                if (openedFilePath![0] !== "/")
                  openedFilePath = "/" + openedFilePath;
              }

              if (process.platform === 'win32') {
                if (sourceFilePath[0] === '/' || sourceFilePath[0] === '\\') {
                  sourceFilePath = sourceFilePath.substring(1);
                }
              }

              if (sourceFilePath === openedFilePath) {
                diagnostics.push(createDiagnostic(doc, fixText, fixRange));
              }
            });
          });
        });
      }
      // we should filter diagnostics that only apply to the current file.
      aiFixCodeDiagnostics.set(doc.uri, diagnostics);
    });
    logging.LogInfo("Finished diagnosis.");
  } catch (error) {
    console.log(error);
    logging.LogErrorAndShowErrorMessage(
      "Unable to run diagnosis on file: " + error,
      "Unable to run diagnosis on file: " + error
    );
  }
}

function createDiagnostic(
  doc: vscode.TextDocument,
  lineOfText: string,
  issueRange: IIssueRange
): vscode.Diagnostic {
  const range = new vscode.Range(
    issueRange.startLine - 1,
    issueRange.startColumn,
    issueRange.endLine - 1,
    issueRange.endColumn
  );

  const diagnostic = new vscode.Diagnostic(
    range,
    lineOfText,
    vscode.DiagnosticSeverity.Information
  );
  diagnostic.code = ANALYZER_MENTION;
  return diagnostic;
}

export function subscribeToDocumentChanges(
  context: vscode.ExtensionContext,
  emojiDiagnostics: vscode.DiagnosticCollection
): void { }
