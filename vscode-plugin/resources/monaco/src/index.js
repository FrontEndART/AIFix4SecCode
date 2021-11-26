import './config';
import './style.css';

// @ts-ignore
import * as monaco from 'monaco-editor';

import { render, addDiffActions, swap, layoutDiffContainer, postSaveMessage } from './utils';

const diffEditor = monaco.editor.createDiffEditor(
  document.getElementById('container'),
  {
    originalEditable: true,
    theme: "vs-dark"
  }
);


self.addEventListener('resize', () => {
  if (diffEditor) {
    diffEditor.layout();
    layoutDiffContainer();
  }
});

self.addEventListener('message', (e) => {
  // @ts-ignore
  const { diffNavigator } = window;
  const {
    data: { key, payload },
  } = e;
  switch (key) {
    case 'data':
      render(diffEditor, payload);
      break;
    case 'nextDiff':
      diffNavigator.next();
      break;
    case 'prevDiff':
      diffNavigator.previous();
      break;
    case 'swap':
      swap();
      break;
    case 'save':
      postSaveMessage();
      break;
  }
});

// @ts-ignore
self.vscode.postMessage({
  command: 'load',
});

diffEditor.onDidUpdateDiff(() => {
  addDiffActions(diffEditor);
});
