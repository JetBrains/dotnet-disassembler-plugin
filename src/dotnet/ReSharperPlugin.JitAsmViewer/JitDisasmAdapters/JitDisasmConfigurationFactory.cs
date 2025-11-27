using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

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
            UseUnloadableContext = config.UseUnloadableContext,
            DontGuessTfm = config.DontGuessTFM,
            SelectedCustomJit = config.SelectedCustomJit ?? JitDisasmConfiguration.DefaultJit,

            // Advanced properties use defaults (not exposed in UI)
            JitDumpInsteadOfDisasm = false,
            UseCustomRuntime = false,
            FgEnable = false,
            CustomEnvVars = null,
            Crossgen2Args = null,
            IlcArgs = null,
            PathToLocalCoreClr = null,
            OverridenJitDisasm = null,
            OverridenTfm = null,
            Arch = "x64"
        };
    }
}
