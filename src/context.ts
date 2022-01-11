import { commands } from 'vscode';

export function setPanelFocused(focused: boolean) {
  commands.executeCommand('setContext', 'aifix4seccode-vscode.panelFocused', focused);
}