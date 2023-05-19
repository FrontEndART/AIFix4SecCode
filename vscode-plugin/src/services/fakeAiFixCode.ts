import { access, constants, watch, readFileSync, readFile } from "fs";
import { dirname, basename } from "path";
import { updateHeritageClause } from "typescript";
import { Terminal, window, ProgressLocation, workspace } from "vscode";
import {
  ANALYZER_EXE_PATH,
  PATCH_FOLDER,
  PROJECT_FOLDER,
  utf8Stream,
} from "../constants";
import * as logging from "../services/logging";
import { IFix, Iissue } from "../interfaces";
import { getSafeFsPath } from "../path";

const fs = require("fs");
const util = require("util");
const parseJson = require("parse-json");
var path = require("path");
var upath = require("upath");
var isEqual = require('lodash.isequal');

//export let issues = '';
let issuesJson: any = {};

export async function getIssues() {
  return issuesJson;
}

export function getIssuesSync() {
  let issuesPath: string | undefined = "";
  if (
    workspace
      .getConfiguration()
      .get<string>("aifix4seccode.analyzer.issuesPath")
  ) {
    issuesPath = workspace
      .getConfiguration()
      .get<string>("aifix4seccode.analyzer.issuesPath");
  }

  // read content of list file - get all the json paths that are in that file and merge them into one json object.
  try {
    var jsonListContent = fs.readFileSync(issuesPath!, utf8Stream);
  } catch (e) {
    if (typeof e === "string") {
      logging.LogErrorAndShowErrorMessage(e.toUpperCase(), e.toUpperCase())
    } else if (e instanceof Error) {
      logging.LogErrorAndShowErrorMessage(e.message, e.message)
      if ('code' in e) {
        if ((e as any).code.toString() === 'ENOENT') {
          var adjustedIssuesPath = upath.join(upath.dirname(ANALYZER_EXE_PATH), 'results', 'jsons.lists')
          jsonListContent = fs.readFileSync(adjustedIssuesPath!, utf8Stream);
        }
      }
    }
  }
  var patchJsonPaths: string[] = []
  if (jsonListContent)
    patchJsonPaths = jsonListContent.split('\n');
  if (patchJsonPaths.length) {
    patchJsonPaths.forEach((path: any) => {
      if (path.length) {
        var patchJson = parseJson(fs.readFileSync(path!, utf8Stream));
        var fileName = path.split(/[\/\\]/).pop().replace(".json", "");
        Object.keys(issuesJson).forEach((key: any) => {
          var i = issuesJson[key].length
          while (i--) {
            if (issuesJson[key][i]["JavaFileName"] === fileName) {
              issuesJson[key].splice(i, 1);
            }
          }
          if (!issuesJson[key].length) {
            delete issuesJson[key];
          }
        });
        Object.keys(patchJson).forEach((key: any) => {
          if (issuesJson!.hasOwnProperty(key)) {
            patchJson[key].forEach((issue: any) => {
              if ((issuesJson as any)[key].indexOf(issue) === -1) {
                issue["JavaFileName"] = fileName;
                (issuesJson as any)[key].push(issue);
              }
            })
          } else {
            (issuesJson as any)[key] = patchJson[key];
            (issuesJson as any)[key].forEach((issue: any) => issue["JavaFileName"] = fileName);
          }
          (issuesJson as any)[key].forEach((issue: any) => issue.patches.sort((a: any, b: any) => b.bscore - a.score));
        })
      }
    });

    Object.keys(issuesJson).forEach((key: any) => {
      issuesJson[key].sort((a: any, b: any) => a["textRange"]["startLine"] - b["textRange"]["startLine"]);
      issuesJson[key].sort((a: any, b: any) => a["JavaFileName"].localeCompare(b["JavaFileName"]));
    });
  }
  return issuesJson;
}

export async function getFixes(leftPath: string, patchPath: string) {
  let issueGroups = await getIssues();
  let fixes: any[] = [];

  if (issueGroups) {
    Object.values(issueGroups).forEach((issues: any) => {
      issues.forEach((issue: any) => {
        if (issue.patches.find((fix: IFix) => fix.path === upath.basename(patchPath))) {
          issue.patches.forEach((fix: IFix) => {
            var patch = "";
            try {
              patch = readFileSync(upath.join(PATCH_FOLDER, fix.path), "utf8");
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

            let patch_folder = PATCH_FOLDER;
            sourceFile = upath.normalize(upath.join(PROJECT_FOLDER, sourceFile));
            leftPath = upath.normalize(leftPath);
            if (process.platform === "linux" || process.platform === "darwin") {
              if (sourceFile[0] !== "/") {
                sourceFile = "/" + sourceFile;
              }
            }

            if (sourceFile === leftPath) {
              fixes.push(fix);
            }
          });
        }
      });
    });
  }

  return fixes;
}

function hasSubArray(master: any, sub: any) {
  return sub.every((i => (v: any) => i = master.indexOf(v, i) + 1)(0));
}
