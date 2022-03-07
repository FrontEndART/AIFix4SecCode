import { ExecSyncOptionsWithStringEncoding } from 'child_process';
import { workspace } from 'vscode';
import { getRootPath } from './path';
var path = require("path");

export let PROJECT_FOLDER = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.subjectProjectPath');

export function SetProjectFolder(path: string){
  PROJECT_FOLDER = path;
}

export const PATCH_FOLDER = path.normalize(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.generatedPatchesPath'));
export const ANALYZER_EXE_PATH = path.normalize(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.executablePath'));
export const ANALYZER_PARAMETERS = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.executableParameters')
export const ANALYZER_USE_DIFF_MODE = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.useDiffMode')
export const ANALYZER_MENTION = 'analyzer_mention';
export const ISSUE = 'issue';

export const UNSAVED_SYMBOL = ' •';
export const fileNotSupported = `The file is not displayed in the editor because it is either binary, uses an unsupported text encoding or it's an empty file`;
export const utf8Stream: ExecSyncOptionsWithStringEncoding = {
  encoding: 'utf8',
};
export const cwdCommandOptions = {
  ...utf8Stream,
  cwd: getRootPath(),
};
