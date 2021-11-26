# AIFix4SecCode-VSCode

## Setup:

Open up a vscode internal terminal and run:
```bash
npm i
```

If a popup says that it's unable to attach a watch debugger, just click on the debug anyway button.

## Packaging, publishing:

We need to install vsce package manager:

```bash
npm install -g vsce
```

## Generate .vsix file:

```bash
$ cd myExtension
$ vsce package
# myExtension.vsix generated
```

## Pusblishing extension to marketplace:

```bash
$ vsce publish
# <publisherID>.myExtension published to VS Code Marketplace
```


