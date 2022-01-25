# AIFix4SecCode-VSCode

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


