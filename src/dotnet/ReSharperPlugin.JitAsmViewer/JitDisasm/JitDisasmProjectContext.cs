namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record JitDisasmProjectContext(
    JitDisasmTargetFramework Tfm,
    string OutputPath,
    string ProjectFilePath,
    string ProjectDirectory,
    string AssemblyName);
