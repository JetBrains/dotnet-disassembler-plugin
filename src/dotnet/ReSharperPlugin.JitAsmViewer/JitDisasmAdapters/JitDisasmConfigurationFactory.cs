using JetBrains.Annotations;
using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

public static class JitDisasmConfigurationFactory
{
    public static JitDisasmConfiguration Create(AsmViewerModel model)
    {
        var config = model.Configuration.Maybe.ValueOrDefault;
        return Create(config);
    }

    public static JitDisasmConfiguration Create([CanBeNull] JitConfiguration config)
    {
        return new JitDisasmConfiguration
        {
            ShowAsmComments = config?.ShowAsmComments ?? true,
            UseTieredJit = config?.UseTieredJit ?? false,
            UsePgo = config?.UsePGO ?? false,
            Diffable = config?.Diffable ?? false,
            RunAppMode = config?.RunAppMode ?? false,
            UseNoRestoreFlag = config?.UseNoRestoreFlag ?? false,
            UseDotnetPublishForReload = config?.UseDotnetPublishForReload ?? false,
            UseDotnetBuildForReload = config?.UseDotnetBuildForReload ?? false,
            UseUnloadableContext = config?.UseUnloadableContext ?? false,
            DontGuessTfm = config?.DontGuessTFM ?? false,
            SelectedCustomJit = config?.SelectedCustomJit ?? JitDisasmConfiguration.DefaultJit,

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
