import * as vscode from "vscode";

export function LogErrorAndShowErrorMessage(log: string, message: string){
    console.log("[ERROR]: " + log);
    vscode.window.showErrorMessage(message);
}

export function LogInfoAndShowInformationMessage(log: string, message: string){
    console.log("[INFO]: " + log);
    vscode.window.showInformationMessage(message);
}

export function LogError(log: string){
    console.log("[ERROR]: " + log);
}

export function LogInfo(log: string){
    console.log("[INFO]: " + log);
}

export function ShowErrorMessage(message: string){
    vscode.window.showErrorMessage(message); 
}

export function ShowInfoMessage(message: string){
    vscode.window.showInformationMessage(message);
}