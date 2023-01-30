import { ExecSyncOptionsWithStringEncoding } from 'child_process';
import { workspace } from 'vscode';
import { getRootPath } from './path';
var path = require("path");
var upath = require("upath");
var os = require('os');

// EXTENSION SETTINGS:
export var PROJECT_FOLDER = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.subjectProjectPath');
export var CONFIG_PATH = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.configPath');
export const PATCH_FOLDER = upath.normalize(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.generatedPatchesPath'));
export const ANALYZER_EXE_PATH = upath.normalize(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.executablePath'));
export const ANALYZER_PARAMETERS = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.executableParameters');
export const ANALYZER_USE_DIFF_MODE = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.useDiffMode');
export const ISSUES_PATH = upath.normalize(workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath'));
export const ANALYZER_MENTION = 'analyzer_mention';
export const ISSUE = 'issue';

export function SetProjectFolder(path: string){
  PROJECT_FOLDER = upath.normalize(path);
  PROJECT_FOLDER_LOG = 'plugin.subject_project_path' + '=' + PROJECT_FOLDER + os.EOL;
}

export function SetConfigPath(path: string){
  CONFIG_PATH = upath.normalize(path);
}

if(!PROJECT_FOLDER){
  SetProjectFolder(upath.normalize(workspace.workspaceFolders![0].uri.path));
  console.log('asdf')
}

if(!CONFIG_PATH){
  SetConfigPath(upath.normalize(upath.join(ANALYZER_EXE_PATH, "config.properties")))
}

if(process.platform === 'win32'){
  if(PROJECT_FOLDER){
    if(PROJECT_FOLDER![0] === '/' || PROJECT_FOLDER![0] === '\\'){
      SetProjectFolder(PROJECT_FOLDER!.substring(1));
    }
  }
}

if(process.platform === 'win32'){
  if(CONFIG_PATH){
    if(CONFIG_PATH![0] === '/' || CONFIG_PATH![0] === '\\'){
      SetProjectFolder(CONFIG_PATH!.substring(1));
    }
  }
}

// lOGS:
export const LOG_HEADING = '\r\n' + '# Vscode-Plugin settings' + '\r\n';
export const ANALYZER_PARAMETERS_LOG = 'plugin.executing_parameters' + '=' + ANALYZER_PARAMETERS + '\r\n';
export const ANALYZER_EXE_PATH_LOG = 'plugin.executable_path' + '=' + ANALYZER_EXE_PATH + '\r\n';
export const PATCH_FOLDER_LOG = 'plugin.generated_patches_path' + '=' + PATCH_FOLDER + '\r\n';
export const ISSUES_PATH_LOG = 'plugin.issues_path' + '=' + ISSUES_PATH + '\r\n';
export var PROJECT_FOLDER_LOG = 'plugin.subject_project_path' + '=' + PROJECT_FOLDER + '\r\n';
export const ANALYZER_USE_DIFF_MODE_LOG = 'plugin.use_diff_mode' + '=' + ANALYZER_USE_DIFF_MODE + '\r\n';

export const UNSAVED_SYMBOL = ' •';
export const fileNotSupported = `The file is not displayed in the editor because it is either binary, uses an unsupported text encoding or it's an empty file`;
export const utf8Stream: ExecSyncOptionsWithStringEncoding = {
  encoding: 'utf8',
};
export const cwdCommandOptions = {
  ...utf8Stream,
  cwd: getRootPath(),
};
