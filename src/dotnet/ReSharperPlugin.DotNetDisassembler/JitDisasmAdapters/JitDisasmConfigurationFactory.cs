using System;
using JetBrains.Rider.Model;
using ReSharperPlugin.DotNetDisassembler.JitDisasm;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasmAdapters;

public static class JitDisasmConfigurationFactory
{
    public static JitDisasmConfiguration Create(JitConfiguration config)
    {
        return new JitDisasmConfiguration
        {
            ShowAsmComments = config.ShowAsmComments,
            UseTieredJit = config.UseTieredJit,
            UsePgo = config.UsePGO,
            Diffable = config.Diffable,
            RunAppMode = config.RunAppMode,
            UseNoRestoreFlag = config.UseNoRestoreFlag,
            UseDotnetPublishForReload = config.UseDotnetPublishForReload,
            UseDotnetBuildForReload = config.UseDotnetBuildForReload,
            SelectedCustomJit = config.SelectedCustomJit ?? JitDisasmConfiguration.DefaultJit,
            OverridenTfm = !string.IsNullOrWhiteSpace(config.TargetFrameworkOverride)
                ? JitDisasmTargetFrameworkFactory.Create(config.TargetFrameworkOverride)
                : null,
            DisassemblyTimeout = TimeSpan.FromSeconds(config.DisassemblyTimeoutSeconds),

            // Advanced properties use defaults (not exposed in UI)
            UseUnloadableContext = false,
            JitDumpInsteadOfDisasm = false,
            UseCustomRuntime = false,
            FgEnable = false,
            CustomEnvVars = null,
            Crossgen2Args = null,
            IlcArgs = null,
            PathToLocalCoreClr = null,
            OverridenJitDisasm = null,
            DontGuessTfm = false,
            Arch = RuntimePlatformUtils.GetCurrentArch()
        };
    }
}
