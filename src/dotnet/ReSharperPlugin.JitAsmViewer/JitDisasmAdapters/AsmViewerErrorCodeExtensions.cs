using JetBrains.Rider.Model;
using JetBrains.Util;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

public static class AsmViewerErrorCodeExtensions
{
    private static readonly ILogger Logger = GetLogger(typeof(AsmViewerErrorCodeExtensions));

    public static ErrorCode ToRdErrorCode(this AsmViewerErrorCode code) => code switch
    {
        // PSI/Navigation errors
        AsmViewerErrorCode.SourceFileNotFound => ErrorCode.SourceFileNotFound,
        AsmViewerErrorCode.PsiSourceFileUnavailable => ErrorCode.PsiSourceFileUnavailable,
        AsmViewerErrorCode.UnsupportedLanguage => ErrorCode.UnsupportedLanguage,
        AsmViewerErrorCode.InvalidCaretPosition => ErrorCode.InvalidCaretPosition,

        // Configuration errors
        AsmViewerErrorCode.PgoNotSupportedForAot => ErrorCode.PgoNotSupportedForAot,
        AsmViewerErrorCode.RunModeNotSupportedForAot => ErrorCode.RunModeNotSupportedForAot,
        AsmViewerErrorCode.TieredJitNotSupportedForAot => ErrorCode.TieredJitNotSupportedForAot,
        AsmViewerErrorCode.FlowgraphsNotSupportedForAot => ErrorCode.FlowgraphsNotSupportedForAot,
        AsmViewerErrorCode.FlowgraphsForClassNotSupported => ErrorCode.FlowgraphsForClassNotSupported,
        AsmViewerErrorCode.UnsupportedTargetFramework => ErrorCode.UnsupportedTargetFramework,
        AsmViewerErrorCode.CustomRuntimeRequiresNet7 => ErrorCode.CustomRuntimeRequiresNet7,

        // Compilation errors
        AsmViewerErrorCode.CompilationFailed => ErrorCode.CompilationFailed,
        AsmViewerErrorCode.ProjectPathNotFound => ErrorCode.ProjectPathNotFound,
        AsmViewerErrorCode.DotnetBuildFailed => ErrorCode.DotnetBuildFailed,
        AsmViewerErrorCode.DotnetPublishFailed => ErrorCode.DotnetPublishFailed,

        // Runtime/Path errors
        AsmViewerErrorCode.DotNetCliNotFound => ErrorCode.DotNetCliNotFound,
        AsmViewerErrorCode.RuntimePackNotFound => ErrorCode.RuntimePackNotFound,
        AsmViewerErrorCode.CoreClrCheckedNotFound => ErrorCode.CoreClrCheckedNotFound,

        // Other errors
        AsmViewerErrorCode.UpdateCancelled => ErrorCode.UpdateCancelled,
        AsmViewerErrorCode.UnknownError => ErrorCode.UnknownError,
        _ => LogAndReturnUnknown(code)
    };

    private static ErrorCode LogAndReturnUnknown(AsmViewerErrorCode code)
    {
        Logger.Error("Unknown error code mapping: {0}", code);
        return ErrorCode.UnknownError;
    }
}
