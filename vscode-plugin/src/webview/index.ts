import { log } from '../logger';
import { writeFileSync } from 'fs';
import { setPanelFocused } from '../context';
import { getSafeFsPath } from '../path';
import { setActiveDiffPanelWebview } from './store';
import { ExtendedWebview, ExtendedWebviewEnv, IExtendedWebviewEnvDiff } from './extendedWebview';
import { utf8Stream, fileNotSupported, PROJECT_FOLDER } from '../constants';
import { window, ViewColumn, ExtensionContext, workspace, Uri, ProgressLocation } from 'vscode';
import { extract } from '../theme/extractor';
import { getTitle } from './utils';
import { Iissue } from '../interfaces';
import { getIssues } from '../services/fakeAiFixCode';
import { analysisDiagnostics } from '../extension';
import { testView } from '../commands';
import { refreshDiagnostics } from '../language/diagnostics';
import { endsWith } from 'lodash';

var stringify = require('json-stringify');

interface IDiffData {
  patchPath?: string;
  leftContent: string;
  rightContent: string;
  leftPath?: string;
  rightPath: string;
  context: ExtensionContext;
  theme?: string;
}

let issues: any;

async function initIssues() {
  issues = await getIssues();
}

const column = ViewColumn.One;

export async function showDiff({ patchPath, leftContent, rightContent, leftPath, rightPath, context }: IDiffData) {
  try {
    const options = {
      enableScripts: true,
      retainContextWhenHidden: true,
    };

    const panel = window.createWebviewPanel(
      'AIFix4SecCode',
      'dark',
      column,
      options
    );

    const theme = await extract();
    const { tabSize } = workspace.getConfiguration('editor');

    const webviewEnv: ExtendedWebviewEnv = {
      patchPath,
      leftPath,
      rightPath,
      leftContent,
      rightContent,
      fileNotSupported,
      theme,
      tabSize
    };

    const extendsWebView = new ExtendedWebview(
      panel,
      'diff',
      context,
      webviewEnv,
      'file',
    );

    extendsWebView.onDidSave(async (e: SaveEvent, env: IExtendedWebviewEnvDiff) => {
      try {
        const { right: rightContent } = e.contents;
        const savedLeftPath = await getSaveLeftPath(env.leftPath!);
        const patchPath = env.patchPath!;
        if (savedLeftPath) {
          writeFileSync(getSafeFsPath(savedLeftPath), rightContent, utf8Stream);
          initIssues().then(() => {
            if (issues) {
              Object.keys(issues).forEach(key => {
                if (issues[key]!.patches.some((x: any) => x.path === patchPath)) {
                  delete issues[key];
                }
              });
            }
            console.log(issues);

            let issuesStr = stringify(issues);
            console.log(issuesStr);

            //writeFileSync('C:/Projects/dummyAnalyzerService/dummyAnalyzer/AnalysisOutput/issues.json', issuesStr, utf8Stream);
            //writeFileSync('E:/Projects/AIFixCode/vscode_plugin_20210804/required/dummyAnalyzerService/dummyAnalyzer/AnalysisOutput/issues.json', issuesStr, utf8Stream);
            let issuesPath : string | undefined = '';
            if(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath')){
              issuesPath = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath');
            }
            writeFileSync(issuesPath!, issuesStr, utf8Stream);

            // refresh:
            testView.treeDataProvider?.refresh('');

            var openFilePath = Uri.parse("file:///" + PROJECT_FOLDER + '/' + env.leftPath!);
            workspace.openTextDocument(openFilePath).then(document => {
              window.showTextDocument(document).then(() => {
                window.withProgress({ location: ProgressLocation.Notification, title: 'Loading Diagnostics...' }, async () => {
                  await refreshDiagnostics(window.activeTextEditor!.document, analysisDiagnostics);
                });
              });
            });

          });

          window.showInformationMessage('Content saved to path: ' + savedLeftPath);
          extendsWebView.webViewPanel.dispose();
          return savedLeftPath;
        }
        return '';
      } catch (error) {
        window.showErrorMessage('Something went wrong with saving this content...');
        log(`Error: can't save file due "${error}"`);
        return '';
      }
    });

    extendsWebView.render();
    panel.onDidChangeViewState(e => {
      setPanelFocused(e.webviewPanel.active);
      log(`panel visibility changed to: ${e.webviewPanel.active}`);
      if (e.webviewPanel.active) {
        setActiveDiffPanelWebview(extendsWebView);
      }
    });
    panel.onDidDispose(() => {
      log('panel disposed');
      setPanelFocused(false);
    });
    setTimeout(() => {
      setPanelFocused(true);
      setActiveDiffPanelWebview(extendsWebView);
    }, 100);
  } catch (error) {
    log(error);
  }
}

async function getSaveLeftPath(path: string): Promise<string> {
  if (!path) {
    const uri = await window.showSaveDialog({});
    if (uri) {
      path = uri.fsPath;
    }
  }
  return path;
}

export function showNotSupported(context: ExtensionContext, rightPath: string, mode: ExtendedWebviewMode) {
  const title = getTitle(rightPath, mode);
  const options = {
    enableScripts: true,
  };
  const panel = window.createWebviewPanel(
    'aifix4seccode-vscode.fileNotSupported',
    title,
    column,
    options
  );

  const webviewEnv: ExtendedWebviewEnv = {
    content: fileNotSupported
  };

  const extendsWebview = new ExtendedWebview(
    panel,
    'notSupported',
    context,
    webviewEnv
  );
  extendsWebview.render();
}