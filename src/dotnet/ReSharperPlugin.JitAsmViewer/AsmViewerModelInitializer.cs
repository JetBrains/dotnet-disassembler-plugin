using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

public static class AsmViewerModelInitializer
{
    public static void Initialize(AsmViewerModel model)
    {
        InitializeSnapshotProperties(model);
        InitializeUiStateProperties(model);
        InitializeLoadingState(model);
        InitializeConfiguration(model);
    }

    private static void InitializeSnapshotProperties(AsmViewerModel model)
    {
        model.SnapshotContent.Value = null;
        model.HasSnapshot.Value = false;
    }

    private static void InitializeUiStateProperties(AsmViewerModel model)
    {
        model.SourceFilePath.Value = null;
        model.CaretOffset.Value = null;
    }

    private static void InitializeLoadingState(AsmViewerModel model)
    {
        model.IsLoading.Value = false;
        model.UnavailabilityReason.Value = null;
    }

    private static void InitializeConfiguration(AsmViewerModel model)
    {
        model.ShowAsmComments.Value = true;
        model.UseTieredJit.Value = false;
        model.UsePGO.Value = false;
        model.Diffable.Value = false;
        model.RunAppMode.Value = false;
        model.UseNoRestoreFlag.Value = false;
        model.UseDotnetPublishForReload.Value = false;
        model.UseDotnetBuildForReload.Value = false;
        model.UseUnloadableContext.Value = false;
        model.DontGuessTFM.Value = false;
        model.SelectedCustomJit.Value = JitDisasmConfiguration.DefaultJit;
    }
}
