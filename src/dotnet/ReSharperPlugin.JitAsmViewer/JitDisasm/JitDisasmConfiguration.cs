using System;
using System.Collections.Generic;
using JetBrains.Core;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record JitDisasmConfiguration
{
    public string PathToLocalCoreClr { get; init; }
    public bool JitDumpInsteadOfDisasm { get; init; }
    public string CustomEnvVars { get; init; }
    public string Crossgen2Args { get; init; }
    public string IlcArgs { get; init; }
    public bool ShowAsmComments { get; init; }
    public bool UpdateIsAvailable { get; init; }
    public Version CurrentVersion { get; init; }
    public Version AvailableVersion { get; init; }
    public bool UseDotnetPublishForReload { get; init; }
    public bool UseDotnetBuildForReload { get; init; }
    public bool RunAppMode { get; init; }
    public bool UseNoRestoreFlag { get; init; }
    public bool UseTieredJit { get; init; }
    public bool UseUnloadableContext { get; init; }
    public bool UsePgo { get; init; }
    public bool Diffable { get; init; }
    public bool DontGuessTfm { get; init; }
    public bool UseCustomRuntime { get; init; }
    public List<string> CustomJits { get; init; }
    public string SelectedCustomJit { get; init; }
    public string OverridenJitDisasm { get; init; }
    public bool FgEnable { get; init; }
    public JitDisasmTargetFramework OverridenTfm { get; init; }
    public string Arch { get; init; } = "x64";
    public TimeSpan DisassemblyTimeout { get; init; } = TimeSpan.FromSeconds(120);
    
    public bool CrossgenIsSelected => SelectedCustomJit?.StartsWith("crossgen") == true;

    public bool NativeAotIsSelected => SelectedCustomJit?.StartsWith("ilc") == true;
    
    public const string DefaultJit = "clrjit.dll";
    public const string Crossgen = "crossgen2.dll (R2R)";
    public const string Ilc = "ilc (NativeAOT)";

    public Result<JitDisasmConfiguration, Error> Validate()
    {
        if (CrossgenIsSelected || NativeAotIsSelected)
        {
            if (UsePgo)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.PgoNotSupportedForAot));

            if (RunAppMode)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.RunModeNotSupportedForAot));

            if (UseTieredJit)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.TieredJitNotSupportedForAot));

            if (FgEnable)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.FlowgraphsNotSupportedForAot));
        }

        return Result.Success(this);
    }
    
    public bool IsNonCustomDotnetAotMode()
    {
        return !UseCustomRuntime &&
               (SelectedCustomJit == Crossgen || SelectedCustomJit == Ilc);
    }

    public bool IsNonCustomNativeAotMode()
    {
        return !UseCustomRuntime && SelectedCustomJit == Ilc;
    }
    
    public void FillWithUserVars(Dictionary<string, string> dictionary)
    {
        if (string.IsNullOrWhiteSpace(CustomEnvVars))
            return;

        var pairs = CustomEnvVars.Split(new [] {"\r\n", "\n"}, StringSplitOptions.RemoveEmptyEntries);
        foreach (var pair in pairs)
        {
            var parts = pair.Split('=');
            if (parts.Length == 2)
                dictionary[parts[0].Trim()] = parts[1].Trim();
        }
    }
};