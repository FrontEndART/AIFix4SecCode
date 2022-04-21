# AIFix4SecCode-VSCode

## Installing dependencies:

```bash
npm install
```

## Packaging, publishing:

If you want to test an extension on your local install of VSCode

We need to install vsce package manager:

```bash
npm install -g vsce
```

## Generate .vsix file:
This file format is used to share the plugin privately with others.
Run vsce package in extension root folder to create such VSIX files.
```bash
$ cd path/to/vscode-plugin
$ vsce package
# aifix4seccode-vscode-x.y.z.vsix generated
```

At new release, you can modify the version at the top of package.json you can modify the version of the plugin:
### (x): major version
Backward compatible bug fixes
### (y): minor version
Backward compatible new features
### (z): patch version
Changes that break backward compatibility
## Installing .vsix plugin file to VSCode:

```bash
code --install-extension aifix4seccode-vscode-x.y.z
```

## Example of usage on a demo project:
Try the Patch_Validation.zip as a demo project to analyze, patch issues, etc.
