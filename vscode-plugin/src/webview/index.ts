import { log } from '../logger';
import { setPanelFocused } from '../context';
import { applyPatchToFile } from '../patch';
import { setActiveDiffPanelWebview } from './store';
import { ExtendedWebview, ExtendedWebviewEnv, IExtendedWebviewEnvDiff } from './extendedWebview';
import { fileNotSupported } from '../constants';
import { window, ViewColumn, ExtensionContext, workspace } from 'vscode';
import { extract } from '../theme/extractor';
import { getTitle } from './utils';
import { getIssues } from '../services/fakeAiFixCode';
var path = require("path");

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
        let output = applyPatchToFile(savedLeftPath, rightContent, patchPath);
        extendsWebView.webViewPanel.dispose();
        return output!;
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
    log(String(error));
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