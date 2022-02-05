import { access, constants, watch, readFileSync, readFile } from "fs";
import { dirname, basename } from "path";
import { Terminal, window, ProgressLocation, workspace } from "vscode";
import { ANALYZER_EXE_PATH, PATCH_FOLDER, PROJECT_FOLDER, utf8Stream } from "../constants";
import { IFix, Iissue } from "../interfaces";
import { getSafeFsPath } from "../path";

const fs = require('fs');
const util = require('util');
const parseJson = require('parse-json');
var path = require("path");

//export let issues = '';
export let issuesJson: Iissue | undefined;

export async function getIssues() {

    const readFile = util.promisify(fs.readFile);
    let issuesPath: string | undefined = '';
    if (workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath')) {
        issuesPath = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath');
    }

    async function loadIssues() {
        return await readFile(issuesPath!, utf8Stream);
    }

    let result = await loadIssues();
    try {
        issuesJson = parseJson(result);
    } catch (err) {
        console.log(err);
    }
    if (issuesJson) {
        if (issuesJson.fixes) {
            issuesJson.fixes = issuesJson.fixes.sort(i => i.score);
        }
    }
    return issuesJson;
}

export function getIssuesSync() {

    let issuesPath: string | undefined = '';
    if (workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath')) {
        issuesPath = workspace.getConfiguration().get<string>('aifix4seccode.analyzer.issuesPath');
    }

    let result = fs.readFileSync(issuesPath!, utf8Stream);
    try {
        issuesJson = parseJson(result);
    } catch (err) {
        console.log(err);
    }
    if (issuesJson) {
        if (issuesJson.fixes) {
            issuesJson.fixes = issuesJson.fixes.sort(i => i.score);
        }
    }
    return issuesJson;
}

export async function getFixes(leftPath: string) {
    let issues = await getIssues();
    let fixes: any[] = [];

    if (issues) {
        Object.values(issues).forEach(issue => {
            issue.patches.forEach((fix: IFix) => {
                var patch = '';
                try {
                    patch = readFileSync(PATCH_FOLDER + '/' + fix.path, "utf8");
                } catch (err) {
                    console.log(err);
                }

                var sourceFileMatch = /--- ([^ \n\r\t]+).*/.exec(patch);
                var sourceFile: string;
                if (sourceFileMatch && sourceFileMatch[1]) {
                    sourceFile = sourceFileMatch[1];
                } else {
                    throw Error("Unable to find source file in '" + fix.path + "'");
                }
                
                if (getSafeFsPath(path.join(PATCH_FOLDER, sourceFile)) === getSafeFsPath(leftPath)) {
                    fixes.push(fix);
                }
            });
        });
    }

    return fixes;
}