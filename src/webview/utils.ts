import { getFilePath } from '../path';
var path = require('path'),
    fs = require('fs');

export function getTitle(rightPath: string, mode: ExtendedWebviewMode, leftPath?: string, leftHasContent: boolean = false) {
  if (leftPath) {
    return `${getFilePath(leftPath)} ↔ ${getFilePath(rightPath)}`;
  } else if (!rightPath) {
    return 'Untitled';
  } else if (mode === 'git') {
    const gitStatus = leftHasContent ? 'Working Tree' : 'Untracked';
    return `${getFilePath(rightPath)} (${gitStatus})`;
  } else {
    return getFilePath(rightPath);
  }
}

export function ensureDirectoryExistence(filePath : string) {
  var dirname = path.dirname(filePath);
  if (fs.existsSync(dirname)) {
    return true;
  }
  ensureDirectoryExistence(dirname);
  fs.mkdirSync(dirname);
}