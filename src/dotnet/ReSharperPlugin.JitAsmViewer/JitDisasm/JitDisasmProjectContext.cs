namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record JitDisasmProjectContext(
    string Sdk,
    JitDisasmTargetFramework Tfm,
    string OutputPath,
    string ProjectFilePath,
    string ProjectDirectory,
    string AssemblyName);
