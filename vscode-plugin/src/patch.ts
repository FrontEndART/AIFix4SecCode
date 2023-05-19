import { refreshDiagnostics } from './language/diagnostics';
import { writeFileSync } from 'fs';
import { getIssues } from './services/fakeAiFixCode';
import { getSafeFsPath } from './path';
import { utf8Stream, PROJECT_FOLDER, ANALYZER_USE_DIFF_MODE } from './constants';
import { env } from 'process';
import { workspace, Uri, window, ProgressLocation } from 'vscode';
import { testView } from './commands';
import { analysisDiagnostics } from './extension';
import { updateUserDecisions } from './commands';


var stringify = require('json-stringify');

let issueGroups: any;

async function initIssues() {
  issueGroups = await getIssues();
}

const pathDescription: RegExp = /((.|\n|\r)*)@@\n/g;
const eof = /\n$/;

type Line = {
  added: boolean;
  removed: boolean;
  eof: boolean;
  code: string;
};

type LineType = keyof Pick<Line, 'added' | 'removed'>;
type Side = 'left' | 'right';

const eofR = /\\ No newline at end of file/;

export function patchToCodes(patch: string) {
  const onlyCode = patch.replace(pathDescription, '').replace(eof, '');
  const patchHasEof = !patch.match(eofR);

  const lines = onlyCode.split('\n').map((line) => ({
    added: line.startsWith('+'),
    removed: line.startsWith('-'),
    code: line.substr(1, line.length),
    eof: !!line.match(eofR),
  }));

  const extractSide = (side: Side) => {
    const lineTypeToRemove: LineType = side === 'left' ? 'added' : 'removed';
    const sideHasEof = !!lines[lines.findIndex((l) => l.eof) - 1]?.[
      lineTypeToRemove
    ];

    const sideLines = lines.reduce<string[]>((acc, curr, idx) => {
      if (curr[lineTypeToRemove] || curr.eof) {
        return acc;
      }
      return [...acc, curr.code];
    }, []);
    if (patchHasEof || sideHasEof) {
      sideLines.push('');
    }

    return sideLines.join('\n');
  };

  const leftContent = extractSide('left');
  const rightContent = extractSide('right');

  return {
    leftContent,
    rightContent,
  };
}

// 1.: Overwrites the file with the patch's fixes.
// 2.: Updates the issues.json so the fix will no longer show up.
// 3.: Opens up the file that has been patched in the editor.
// 4.: Refreshes the diagnosis on the file to show the remaining suggestions.
export function applyPatchToFile(leftPath: string, rightContent: string, patchPath: string) {
  if (leftPath) {

    if (!rightContent) {
      window.showErrorMessage('Failed to apply patch to source file! \n Make sure that your configuration is correct. Also make sure that the source file has not been patched already by this patch before! This issue may occour if the patch syntax is incorrect.');
      return;
    }

    // 1.
    writeFileSync(leftPath, rightContent, utf8Stream);
    // 2.
    initIssues().then(() => {
      if (issueGroups) {
        Object.values(issueGroups).forEach((issues: any) => {
          issues.forEach((issue: any) => {
            if (issue.patches.some((x: any) => x.path === patchPath || patchPath.includes(x.path))) {
              delete issueGroups[issues];
            }
          });
        });
      }
      console.log(issueGroups);

      let issuesStr = stringify(issueGroups);
      console.log(issuesStr);

      let issuesPath: string | undefined = '';
      if (workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath')) {
        issuesPath = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath');
      }
      writeFileSync(issuesPath!, issuesStr, utf8Stream);

      // 3.
      workspace.openTextDocument(leftPath).then(document => {
        window.showTextDocument(document).then(() => {
          window.withProgress({ location: ProgressLocation.Notification, title: 'Loading Diagnostics...' }, async () => {
            // 4.
            //await refreshDiagnostics(window.activeTextEditor!.document, analysisDiagnostics);

            // User decisions are updated here in patch mode (and at extendedWebview in diff mode):
            if (ANALYZER_USE_DIFF_MODE == "view Patch files") {
              updateUserDecisions('applied', patchPath, leftPath);
            }
          });
        });
      });

    });

    window.showInformationMessage('Content saved to path: ' + leftPath);
    return leftPath;
  }
  return '';
}
