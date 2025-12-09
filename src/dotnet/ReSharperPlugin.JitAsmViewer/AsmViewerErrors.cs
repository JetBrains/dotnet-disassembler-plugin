using JetBrains.Core;

namespace ReSharperPlugin.JitAsmViewer;

public record Error(AsmViewerErrorCode Code, string Details = null)
{
    public static implicit operator Error(AsmViewerErrorCode code) => new(code);

    public static implicit operator Result<Nothing, Error>(Error error) =>
        Result.FailWithValue(error);
}

public enum AsmViewerErrorCode
{
    // PSI/Navigation errors
    SourceFileNotFound,
    PsiSourceFileUnavailable,
    UnsupportedLanguage,
    InvalidCaretPosition,

    // Configuration errors
    PgoNotSupportedForAot,
    RunModeNotSupportedForAot,
    TieredJitNotSupportedForAot,
    FlowgraphsNotSupportedForAot,
    FlowgraphsForClassNotSupported,
    UnsupportedTargetFramework,
    CustomRuntimeRequiresNet7,

    // Compilation errors
    DisassemblyTargetNotFound,
    CompilationFailed,
    ProjectPathNotFound,
    DotnetBuildFailed,
    DotnetPublishFailed,

    // Runtime/Path errors
    DotNetCliNotFound,
    RuntimePackNotFound,
    CoreClrCheckedNotFound,
    ClrJitNotFound,

    // Other errors
    UpdateCancelled,
    UnknownError
}
