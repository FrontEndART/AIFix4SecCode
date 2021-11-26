import { ExtendedWebview } from './extendedWebview';

let activeDiffPanelWebview: ExtendedWebview;
let activeDiffPanelWebviews: ExtendedWebview[] = [];

export function setActiveDiffPanelWebview(webview: ExtendedWebview) {
  activeDiffPanelWebview = webview;

  if(!activeDiffPanelWebviews.includes(webview)){
    activeDiffPanelWebviews.push(webview);
  }
}

export function getActiveDiffPanelWebview() {
  return activeDiffPanelWebview;
}

export function getActiveDiffPanelWebviews(){
  return activeDiffPanelWebviews;
}
