# JIT Assembly Viewer for Rider

<!-- Plugin description -->
**Experimental plugin** that displays JIT-compiled assembly code for C# methods directly in JetBrains Rider.

![ASM Viewer Tool Window](https://raw.githubusercontent.com/JetBrains/JitAsmViewer/master/images/asm-viewer-tool-window.png)

## Features

- View JIT-compiled assembly for any C# method
- Configurable JIT optimization settings (tiered compilation, PGO, diffable output)
- Multiple codegen modes: standard JIT, ReadyToRun (crossgen2), or NativeAOT (ilc)
- Snapshot and diff view to compare assembly output after code changes
- Syntax highlighting for assembly code

## How to Use

1. Place the caret on any C# method, property, constructor, or type declaration in your .NET 6.0+ project
2. Open the JIT Assembly Viewer tool window (View → Tool Windows → ASM Viewer)
3. The plugin will automatically compile and display the JIT assembly for the selected method
4. Configure JIT options via the toolbar to see how different settings affect the generated code
5. Use "Create Snapshot" button to save current assembly, then modify your C# code to see the diff

## Requirements

- .NET SDK 6.0 or higher
- Works only with .NET Core/6+ projects (not .NET Framework)

## Credits

This plugin is based on the [Disasmo](https://github.com/EgorBo/Disasmo) project by Egor Bogatov.

## Feedback

This is an experimental plugin. Please report issues and suggestions on [GitHub](https://github.com/JetBrains/JitAsmViewer/issues).
<!-- Plugin description end -->

## Installation

1. Open Rider
2. Go to `Settings` → `Plugins` → `Marketplace`
3. Search for "JIT Assembly Viewer"
4. Click `Install` and restart Rider

## Usage

1. Open any C# file in a .NET 6.0+ project
2. Place the caret on a method, property, constructor, or type declaration you want to analyze
3. Open the tool window: `View` → `Tool Windows` → `ASM Viewer`
4. The plugin will automatically compile and display the JIT assembly for the selected method
5. Use the toolbar to configure JIT options and see how they affect the generated code
6. Create a snapshot to save the current assembly output, then modify your C# code to see the diff

## Credits

This plugin is based on the [Disasmo](https://github.com/EgorBo/Disasmo) project by [Egor Bogatov](https://github.com/EgorBo).

## Feedback & Contributions

This is an experimental plugin. Please report issues, bugs, and feature requests on [GitHub Issues](https://github.com/JetBrains/JitAsmViewer/issues).

Contributions are welcome! Feel free to submit pull requests.

## License

[Apache License 2.0](LICENSE)
