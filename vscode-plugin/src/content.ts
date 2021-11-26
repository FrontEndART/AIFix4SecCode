import { readFileSync } from 'fs';
import * as istextorbinary from 'istextorbinary';
import { utf8Stream, fileNotSupported, cwdCommandOptions } from './constants';

export function getExplorerSides(leftPath: string, rightPath: string) {
  const leftContent = getContentOrFallback(leftPath) || fileNotSupported;
  const rightContent = getContentOrFallback(rightPath) || fileNotSupported;

  return { leftContent, rightContent };
}

export function getContentOrFallback(path: string) {
  const content = readFileSync(path);
  if (!istextorbinary.isTextSync(undefined, content)) {
    return '';
  }
  return content.toString(utf8Stream.encoding);
}
