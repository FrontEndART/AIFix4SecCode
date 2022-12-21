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
At first use, head over to the extension settings and enter appropriate data:
* ``Executable Parameters``: Specifies the analyzer tool's executable parameters. (f.e.: java -jar Main.jar)
* ``Executable Path``: Specifies the analyzer tool's folder where the executable (.jar, .exe, ...) is located.
* ``Use Diff Mode``: Change the mode of showing patches in the editor.
## Diff mode options:
Mode | Description
------------ | -------------
view Diffs ``default`` | Choosing a fix will show a <b>side-to-side diff view</b> of the original content and the content that the fix would give.
view Patch files | Choosing a fix will show the <b>patch file's content</b> of the fix.

## Examples:
>Try the ``test-project.zip`` as a demo project to analyze, patch issues, etc.
