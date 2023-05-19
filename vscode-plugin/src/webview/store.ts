import { ExtendedWebview } from './extendedWebview';
import * as vscode from "vscode";

let activeDiffPanelWebview: ExtendedWebview;
let activeDiffPanelWebviews: ExtendedWebview[] = [];

export function setActiveDiffPanelWebview(webview: ExtendedWebview) {
  activeDiffPanelWebview = webview;

  if (!activeDiffPanelWebviews.includes(webview)) {
    activeDiffPanelWebviews.push(webview);

    // If a webview in this array will later on be disposed,
    // then we remove it from the active webviews array:
    webview.webViewPanel.onDidDispose(() => {
      const index = activeDiffPanelWebviews.indexOf(webview, 0);
      if (index > -1) {
        activeDiffPanelWebviews.splice(index, 1);
      }
      if (activeDiffPanelWebviews.length === 0) {
        vscode.commands.executeCommand(
          "setContext",
          "patchApplyEnabled",
          false
        );
      }
    })
  }
}

export function getActiveDiffPanelWebview() {
  return activeDiffPanelWebview;
}

export function getActiveDiffPanelWebviews() {
  return activeDiffPanelWebviews;
}
