export interface IProjectAnalysis {
    projectPath: string;
    issues: Iissue[];
}

export interface IChange{
    content: string;
    del: boolean;
    ln: number;
    type: string;
}

export interface Iissue {
    fixes: IFix[];
}

export interface IFix {
    path: string;
    explanation: string;
    score: number;
    textRange: IIssueRange;
}

export interface IIssueRange{
    startLine: number;
    startColumn: number;
    endLine: number;
    endColumn: number;
}

