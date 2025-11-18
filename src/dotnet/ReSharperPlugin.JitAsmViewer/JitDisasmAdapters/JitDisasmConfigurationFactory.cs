using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

public static class JitDisasmConfigurationFactory
{
    public static JitDisasmConfiguration Create(AsmViewerModel model)
    {
        return new JitDisasmConfiguration
        {
            ShowAsmComments = model.ShowAsmComments.Value,
            UseTieredJit = model.UseTieredJit.Value,
            UsePgo = model.UsePGO.Value,
            Diffable = model.Diffable.Value,
            RunAppMode = model.RunAppMode.Value,
            UseNoRestoreFlag = model.UseNoRestoreFlag.Value,
            UseDotnetPublishForReload = model.UseDotnetPublishForReload.Value,
            UseDotnetBuildForReload = model.UseDotnetBuildForReload.Value,
            UseUnloadableContext = model.UseUnloadableContext.Value,
            DontGuessTfm = model.DontGuessTFM.Value,
            SelectedCustomJit = model.SelectedCustomJit.Value,

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
