import * as vscode from "vscode";
import { IFix } from "../interfaces";
import * as path from "path";
import { getIssues } from "../services/fakeAiFixCode";
import { objectify } from "tslint/lib/utils";
import { isObjectLiteralExpression } from "typescript";
import { writeFileSync } from "fs";
import { workspace } from "vscode";
import * as logging from "../services/logging";
import { ANALYZER_EXE_PATH, PATCH_FOLDER, PROJECT_FOLDER, utf8Stream } from "../constants";
var stringify = require("json-stringify");
const util = require("util");
const fs = require("fs");
const parseJson = require("parse-json");
var upath = require("upath");
var isEqual = require('lodash.isequal');
import { readFileSync } from 'fs';

let tree: any;

let revealChangeHandle : vscode.Disposable | null = null;
let changeTitleRevealHandle : vscode.Disposable | null = null;
export class TestView {
  public treeDataProvider: NodeWithIdTreeDataProvider | undefined;
  constructor(context: vscode.ExtensionContext) {
    initTree().then(() => {
      this.treeDataProvider = new NodeWithIdTreeDataProvider();
      const view = vscode.window.createTreeView("testView", {
        treeDataProvider: this.treeDataProvider,
        showCollapseAll: true,
      });
      context.subscriptions.push(view);
      // Check if commands already exist:
      var existingRevealCommand = false;
      var existingChangeTitleCommand = false;

      var extension = vscode.extensions.getExtension('searchlab.aifix4seccode-vscode');
        let commands = extension!.packageJSON.contributes?.commands;
        if (Array.isArray(commands)) {
          existingRevealCommand = commands.find((command: any) => command.command == 'testView.reveal') !== undefined
          existingChangeTitleCommand = commands.find((command: any) => command.command == 'testView.changeTitle') !== undefined
        }
      if(existingRevealCommand){
        if(revealChangeHandle){
          (revealChangeHandle as any).dispose();
          revealChangeHandle = null;
        }
      }
      if(existingChangeTitleCommand){
        if(changeTitleRevealHandle){
          (changeTitleRevealHandle as any).dispose();
          changeTitleRevealHandle = null;
        }
      }
      revealChangeHandle = vscode.commands.registerCommand("testView.reveal", async () => {
        const key = await vscode.window.showInputBox({
          placeHolder: "Type the label of the item to reveal",
        });
        if (key) {
          await view.reveal(
            { key },
            { focus: true, select: false, expand: true }
          );
        }
      });
      changeTitleRevealHandle = vscode.commands.registerCommand("testView.changeTitle", async () => {
        const title = await vscode.window.showInputBox({
          prompt: "Type the new title for the Test View",
          placeHolder: view.title,
        });
        if (title) {
          view.title = title;
        }
      });
    });
  }
}

async function initTree() {
  tree = await getIssues();
  //groupTreeToDistinctGroups(tree);
  console.log(tree);
}

function groupTreeToDistinctGroups(tree: any) {
  Object.keys(tree).forEach((key) => {
    let patchesOfKey = getAllPatchesOfKey(key);
    if (tree[key]) {
      if (tree[key].patches.length < patchesOfKey.length) {
        tree[key].patches = patchesOfKey;
        deleteDuplicateKeys(key);
      }
    }
  });
}

function getAllPatchesOfKey(_key: any) {
  let duplicateKey = "";
  let patches: any = [];
  if (_key.includes("#")) {
    duplicateKey = _key.substring(0, _key.indexOf("#"));
  }

  Object.keys(tree).forEach((key) => {
    if (key.includes("#")) {
      if (key.substring(0, key.indexOf("#")) === duplicateKey) {
        patches = patches.concat(tree[key].patches);
      }
    }
  });

  return patches;
}

function deleteDuplicateKeys(_key: any) {
  let counter = 0;

  Object.keys(tree).forEach((key) => {
    let _keyWithoutId = _key.substring(0, _key.indexOf("#"));
    let keyWithoutId = key.substring(0, key.indexOf("#"));
    if (counter > 0 && _keyWithoutId === keyWithoutId) {
      delete tree[key];
    }

    counter++;
  });
}

let nodes: string[] = [];
let counter = 1;

class NodeWithIdTreeDataProvider
  implements vscode.TreeDataProvider<{ key: string }>
{
  private _onDidChangeTreeData: vscode.EventEmitter<
    { key: string } | undefined | null | void
  > = new vscode.EventEmitter<{ key: string } | undefined | null | void>();
  readonly onDidChangeTreeData: vscode.Event<
    { key: string } | undefined | null | void
  > = this._onDidChangeTreeData.event;

  refresh(patchPath: string): void {
    if (patchPath && patchPath !== "") {
      filterTree(patchPath);
    }
    this._onDidChangeTreeData.fire();
  }

  refreshSubTree(openedPatchPath: string): void {
    updateTreeWithSubTree(openedPatchPath);
    this._onDidChangeTreeData.fire();
  }

  getTreeItem(element: { key: string }): vscode.TreeItem {
    const treeItem = getTreeItem(element.key);
    treeItem.id = (++counter).toString();
    return treeItem;
  }

  getChildren(element: { key: string }): { key: string }[] {
    let children = getChildren(element ? element.key : undefined!);
    let childrenNodes = children.map((key: any) => getNode(key));
    return childrenNodes;
  }

  getParent({ key }: { key: string }): { key: string } {
    const parentKey = key.substring(0, key.length - 1);
    return (parentKey ? new Key(parentKey) : void 0)!;
  }
}

function getChildren(key: string) {
  if (!key) {
    return Object.keys(tree);
  } else {
    //tree[key].map((issue: any) => patches = patches.concat(issue.patches.map((patch: any) => patch["path"])))
    return Object.keys(tree[key]).map((index: any) => key + '#' + (parseInt(index) + 1).toString());
  }
}

function getTreeItem(key: string): vscode.TreeItem {
  const treeElement = getTreeElement(key);
  const tooltip = new vscode.MarkdownString(
    `$(zap) Click to show the source of ${key}`,
    true
  );

  if (Array.isArray(treeElement)) {
    let itemLabel = "";
    let labelText = (treeElement.length > 1) ? key + ' (' + treeElement.length + ' issues)' : key + ' (' + treeElement.length + ' issue)';
    if (treeElement) {
      itemLabel = <any>{
        label: labelText,
        highlights:
          key.length > 1 ? [[key.length - 2, key.length - 1]] : void 0,
      };
    }
    return {
      label: itemLabel,
      tooltip,
      command: {
        title: "Open patch",
        command: "aifix4seccode-vscode.openUpFile",
        arguments: [treeElement[0].patches[0].path],
      },
      collapsibleState: vscode.TreeItemCollapsibleState.Collapsed,
      iconPath: {
        light: path.join(
          __filename,
          "..",
          "..",
          "..",
          "resources",
          "icons",
          "light",
          "eye.svg"
        ),
        dark: path.join(
          __filename,
          "..",
          "..",
          "..",
          "resources",
          "icons",
          "dark",
          "eye.svg"
        ),
      },
    };
  } else {
    let itemLabel = "";
    if (treeElement) {
      itemLabel = <any>{
        label: key,
        highlights:
          key.length > 1 ? [[key.length - 2, key.length - 1]] : void 0,
      };
    }
    return {
      label: itemLabel,
      tooltip,
      command: {
        title: "Open patch",
        command: "aifix4seccode-vscode.openUpFile",
        arguments: [treeElement.path],
      },
      collapsibleState: vscode.TreeItemCollapsibleState.None,
      iconPath: {
        light: path.join(
          __filename,
          "..",
          "..",
          "..",
          "resources",
          "icons",
          "light",
          "screwdriver.svg"
        ),
        dark: path.join(
          __filename,
          "..",
          "..",
          "..",
          "resources",
          "icons",
          "dark",
          "screwdriver.svg"
        ),
      },
    };
  }
}

function getTreeElement(element: any) {
  if (!isNaN(element)) {
    return undefined;
  }
  let parent = tree;
  parent = parent[element];
  if (!parent) {
    //
    let issues: any = Object.values(tree);
    //let patch = undefined;
    //let i = 0;
    //while (patch === undefined) {
      //patch = issues[i].map((issue: any) => issue.patches.find((patch: any) => patch.path === element)).filter((x: any) => x !== undefined)[0]
      //i++;
    //}
    return issues[Object.keys(tree).indexOf(element.split('#')[0])][element.split('#')[1] - 1].patches[0];
  }
  return parent;
}

function getNode(key: any): { key: string } {
  if (!nodes.includes(key)) {
    nodes.push(key);
  }
  return { key: nodes[nodes.indexOf(key)] };
}

function filterTree(patchPath: string) {
  Object.keys(tree).forEach((key) => {
    if(Array.isArray(tree[key])){
      tree[key].forEach((issue: any) => {
          issue.patches.forEach((patch: any) => {
              if(patch.path === patchPath || patchPath.includes(patch.path))
              {
                    tree[key].splice(tree[key].indexOf(issue), 1);
                    if(!tree[key].length){
                        delete tree[key];
                    }
                }
          })
      })
  }
    let issuesStr = stringify(tree);

    let issuesPath: string | undefined = "";
    if (
      vscode.workspace
        .getConfiguration()
        .get<string>("aifix4seccode.analyzer.issuesPath")
    ) {
      issuesPath = vscode.workspace
        .getConfiguration()
        .get<string>("aifix4seccode.analyzer.issuesPath");
    }
    //writeFileSync(issuesPath!, issuesStr, utf8Stream);
});
}

function updateTreeWithSubTree(openedPatchPath: string){
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

  // delete all previous tree-items that are pointing to the file:
  Object.values(tree).forEach((issueGroup: any) => {
    issueGroup.forEach((issue: any) => {
      issue.patches.forEach((patchObj: any) => {
        var patchPath = patchObj.path;

        if(process.platform === 'win32'){
            if(openedPatchPath[0] === '/' || openedPatchPath[0] === '\\'){
              openedPatchPath = openedPatchPath.substring(1);
            }
            if(patchPath[0] === '/' || patchPath[0] === '\\'){
              patchPath = patchPath.substring(1);
            }
        }

        if(patchPath === openedPatchPath || patchPath === upath.basename(openedPatchPath)){
          filterTree(patchPath);
        }
      })
    })
  })
  try{
    var jsonListContent = fs.readFileSync(issuesPath!, utf8Stream);
  } catch (e){
    if (typeof e === "string") {
      logging.LogErrorAndShowErrorMessage(e.toUpperCase(), e.toUpperCase())
    } else if (e instanceof Error) {
      logging.LogErrorAndShowErrorMessage(e.message, e.message)
      if('code' in e){
        if((e as any).code.toString() === 'ENOENT'){
          var adjustedIssuesPath = upath.join(upath.dirname(ANALYZER_EXE_PATH), 'results', 'jsons.lists')
          jsonListContent = fs.readFileSync(adjustedIssuesPath!, utf8Stream);
        }
      }
    }
  }
  var patchJsonPaths: string[] = []
  if (jsonListContent)
    patchJsonPaths = jsonListContent.split('\n');
  var _issuesJson : any = {}
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
    Object.keys(_issuesJson).forEach((key: any) => {
      if(tree.hasOwnProperty(key)){
        _issuesJson[key].forEach((_issue:any) => {
            if(!tree[key].some((treeIssue:any) => isEqual(treeIssue, _issue))){
              tree[key].push(_issue);
            }
          })
      } else {
        tree[key] = _issuesJson[key]
      }
    })
  }
  // tree = {...tree, ...patchJson};
}
class Key {
  constructor(readonly key: string) {}
}
