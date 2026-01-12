using JetBrains.Core;

namespace ReSharperPlugin.DotNetDisassembler;

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
    GenericMethodsRequireRunMode,

    // Compilation errors
    CompilationFailed,
    ProjectPathNotFound,
    DotnetBuildFailed,
    DotnetPublishFailed,

    // Runtime/Path errors
    DotNetCliNotFound,
    RuntimePackNotFound,
    CoreClrCheckedNotFound,

    // Other errors
    DisassemblyTimeout,
    UpdateCancelled,
    UnknownError
}
