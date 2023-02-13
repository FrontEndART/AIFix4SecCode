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
import { IFix, Iissue } from "../interfaces";
import { getSafeFsPath } from "../path";

const fs = require("fs");
const util = require("util");
const parseJson = require("parse-json");
var path = require("path");
var upath = require("upath");

//export let issues = '';
export let issuesJson = {};

export async function getIssues() {
  const readFile = util.promisify(fs.readFile);
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

  async function loadIssues() {
    return await readFile(issuesPath!, utf8Stream);
  }

  var jsonListContent = await loadIssues();
  var patchJsonPaths = jsonListContent.split('\n');
  var _issuesJson = {};
  if (patchJsonPaths.length){
    patchJsonPaths.forEach((path:any) => {
      if(path.length){
        var patchJson = parseJson(fs.readFileSync(path!, utf8Stream));
        Object.keys(patchJson).forEach((key:any) => {
          if(_issuesJson!.hasOwnProperty(key)){
            patchJson[key].forEach((issue: any) => {
              if((_issuesJson as any)[key].indexOf(issue) === -1){
                (_issuesJson as any)[key].push(issue);
              }
            })
          } else {
            (_issuesJson as any)[key] = patchJson[key];
          }
        })
      }
    });
    issuesJson = {...issuesJson, ..._issuesJson}
    
  // let result = await loadIssues();
  // try {
  //   issuesJson = parseJson(result);
  // } catch (err) {
  //   console.log(err);
  // }
  // if (issuesJson) {
  //   if (issuesJson.fixes) {
  //     issuesJson.fixes = issuesJson.fixes.sort((i) => i.score);
  //   }
  // }
  // return issuesJson;
}
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
  // issuesJson = {};
  var jsonListContent = fs.readFileSync(issuesPath!, utf8Stream);
  var patchJsonPaths = jsonListContent.split('\n');
  var _issuesJson = {};
  if (patchJsonPaths.length){
    patchJsonPaths.forEach((path:any) => {
      if(path.length){
        var patchJson = parseJson(fs.readFileSync(path!, utf8Stream));
        Object.keys(patchJson).forEach((key:any) => {
          if(_issuesJson!.hasOwnProperty(key)){
            patchJson[key].forEach((issue: any) => {
              if((_issuesJson as any)[key].indexOf(issue) === -1){
                (_issuesJson as any)[key].push(issue);
              }
            })
          } else {
            (_issuesJson as any)[key] = patchJson[key];
          }
        })
      }
    });
    issuesJson = {...issuesJson, ..._issuesJson}
  //let result = fs.readFileSync(issuesPath!, utf8Stream);
  // try {
  //   issuesJson = parseJson(result);
  // } catch (err) {
  //   console.log(err);
  // }
  // if (issuesJson) {
  //   if (issuesJson.fixes) {
  //     issuesJson.fixes = issuesJson.fixes.sort((i) => i.score);
  //   }
  // }
  // return issuesJson;
  }
  return issuesJson;
}

export async function getFixes(leftPath: string, patchPath: string) {
  let issueGroups = await getIssues();
  let fixes: any[] = [];

  if (issueGroups) {
    Object.values(issueGroups).forEach((issues: any) => {
      issues.forEach((issue: any) => {
        if(issue.patches.find((fix: IFix) => fix.path === upath.basename(patchPath))){
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
