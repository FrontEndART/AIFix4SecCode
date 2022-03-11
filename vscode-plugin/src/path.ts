import { relative, isAbsolute } from 'path';
import { workspace } from 'vscode';

var upath = require('upath');

export function getRootPath(): string {
  return workspace.workspaceFolders ? workspace.workspaceFolders[0].uri.fsPath : '';
}

export function getFilePath(path: string): string {
  if (workspace.workspaceFolders && path.includes(workspace.workspaceFolders[0].uri.fsPath)) {
    return relative(workspace.workspaceFolders[0].uri.fsPath, path);
  }
  return path;
}

export function getSafePath(path: string): string {
  // linux: place trailing slashes at the beginning of the path if they are missing:
  if(process.platform === 'linux' && path[0] !== upath.sep)
    path = upath.sep + path

  // win32: remove trailing slashes at the beginning of the path if they are there:
  if(process.platform === 'win32' && path[0] === upath.sep)
    path = path.substring(1)
    
  return path
}

export function getSafeFsPath(path: string): string {
  path = path.replace(/\//g, '\\')
  if (isAbsolute(path)) {
    return path;
  }
  return `${getRootPath()}/${getFilePath(path)}`;
}