using JetBrains.Core;

namespace ReSharperPlugin.JitAsmViewer;

public record Error(AsmViewerErrorCode Code, string Details = null)
{
    public string Message => string.IsNullOrEmpty(Details)
        ? Code.ToMessage()
        : $"{Code.ToMessage()}: {Details}";

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
    UnsupportedTargetFramework,
    CustomRuntimeRequiresNet7,

    // Compilation errors
    DisassemblyTargetNotFound,
    CompilationFailed,
    DotnetBuildFailed,
    DotnetPublishFailed,

    // Runtime/Path errors
    RuntimePackNotFound,
    CoreClrCheckedNotFound,
    ClrJitNotFound,

    // Other errors
    UpdateCancelled,
    UnknownError
}

public static class AsmViewerErrorCodeExtensions
{
    public static string ToMessage(this AsmViewerErrorCode errorCode)
    {
        return errorCode switch
        {
            // PSI/Navigation errors
            AsmViewerErrorCode.SourceFileNotFound => "Source file not found in solution",
            AsmViewerErrorCode.PsiSourceFileUnavailable => "Unable to get PSI source file",
            AsmViewerErrorCode.UnsupportedLanguage => "ASM Viewer only works with C# files",
            AsmViewerErrorCode.InvalidCaretPosition => "Place caret on a method, property, or constructor to view ASM code",

            // Configuration errors
            AsmViewerErrorCode.PgoNotSupportedForAot => "PGO has no effect on R2R'd/NativeAOT code.",
            AsmViewerErrorCode.RunModeNotSupportedForAot => "Run mode is not supported for crossgen/NativeAOT",
            AsmViewerErrorCode.TieredJitNotSupportedForAot => "TieredJIT has no effect on R2R'd/NativeAOT code.",
            AsmViewerErrorCode.FlowgraphsNotSupportedForAot => "Flowgraphs are not tested with crossgen2/NativeAOT yet (in plugin)",
            AsmViewerErrorCode.UnsupportedTargetFramework => "Only net6.0 (and newer) apps are supported.\nMake sure <TargetFramework>net6.0</TargetFramework> is set in your csproj.",
            AsmViewerErrorCode.CustomRuntimeRequiresNet7 => "Only net7.0 (and newer) apps are supported with non-locally built dotnet/runtime.\nMake sure <TargetFramework>net7.0</TargetFramework> is set in your csproj.",

            // Compilation errors
            AsmViewerErrorCode.DisassemblyTargetNotFound => "Unable to determine disassembly target",
            AsmViewerErrorCode.CompilationFailed => "Failed to generate ASM code",
            AsmViewerErrorCode.DotnetBuildFailed => "dotnet build failed",
            AsmViewerErrorCode.DotnetPublishFailed => "dotnet publish failed",

            // Runtime/Path errors
            AsmViewerErrorCode.RuntimePackNotFound => "Runtime pack not found",
            AsmViewerErrorCode.CoreClrCheckedNotFound => "CoreClr checked files not found",
            AsmViewerErrorCode.ClrJitNotFound => "clrjit.dll not found",

            // Other errors
            AsmViewerErrorCode.UpdateCancelled => "Update cancelled",
            AsmViewerErrorCode.UnknownError => "Unknown error occurred",

            _ => "An error occurred"
        };
    }
}
