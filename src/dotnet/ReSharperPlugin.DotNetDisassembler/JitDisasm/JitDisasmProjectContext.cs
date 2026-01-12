using JetBrains.Annotations;
using JetBrains.Core;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public record JitDisasmProjectContext(
    [CanBeNull] string Sdk,
    JitDisasmTargetFramework Tfm,
    string OutputPath,
    string ProjectFilePath,
    string ProjectDirectory,
    [CanBeNull] string AssemblyName,
    string DotNetCliExePath)
{
    public Result<JitDisasmProjectContext, Error> Validate()
    {
        if (Tfm == null)
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnsupportedTargetFramework, "Target framework not found"));

        if (string.IsNullOrWhiteSpace(OutputPath))
            return Result.FailWithValue(new Error(AsmViewerErrorCode.ProjectPathNotFound, "Output path not found"));

        if (string.IsNullOrWhiteSpace(ProjectFilePath))
            return Result.FailWithValue(new Error(AsmViewerErrorCode.ProjectPathNotFound, "Project file path not found"));

        if (string.IsNullOrWhiteSpace(ProjectDirectory))
            return Result.FailWithValue(new Error(AsmViewerErrorCode.ProjectPathNotFound, "Project directory not found"));

        if (string.IsNullOrWhiteSpace(DotNetCliExePath))
            return Result.FailWithValue(new Error(AsmViewerErrorCode.DotNetCliNotFound));

        return Result.Success(this);
    }
}
