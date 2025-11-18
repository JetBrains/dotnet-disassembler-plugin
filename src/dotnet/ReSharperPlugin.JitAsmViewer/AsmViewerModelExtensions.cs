using System;
using JetBrains.Collections.Viewable;
using JetBrains.Lifetimes;
using JetBrains.Rider.Model;

namespace ReSharperPlugin.JitAsmViewer;

public static class AsmViewerModelExtensions
{
    public static void AdviseRefreshTriggers(this AsmViewerModel model, Lifetime lifetime, Action handler)
    {
        void Subscribe<T>(IViewableProperty<T> property) => property.Advise(lifetime, _ => handler());

        Subscribe(model.SourceFilePath);
        Subscribe(model.CaretOffset);
        Subscribe(model.IsVisible);

        Subscribe(model.ShowAsmComments);
        Subscribe(model.UseTieredJit);
        Subscribe(model.UsePGO);
        Subscribe(model.Diffable);
        Subscribe(model.RunAppMode);
        Subscribe(model.UseNoRestoreFlag);
        Subscribe(model.UseDotnetPublishForReload);
        Subscribe(model.UseDotnetBuildForReload);
        Subscribe(model.UseUnloadableContext);
        Subscribe(model.DontGuessTFM);
        Subscribe(model.SelectedCustomJit);
    }
}
