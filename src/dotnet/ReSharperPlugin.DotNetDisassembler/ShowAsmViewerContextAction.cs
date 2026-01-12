using System;
using System.Collections.Generic;
using JetBrains.Application.Progress;
using JetBrains.Application.UI.Controls.BulbMenu.Anchors;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Bulbs;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Feature.Services.Intentions;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Feature.Services.Resources;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Model;
using JetBrains.TextControl;
using JetBrains.Util;
using ReSharperPlugin.DotNetDisassembler.JitDisasm;
using Strings = ReSharperPlugin.DotNetDisassembler.Resources.Strings;

namespace ReSharperPlugin.DotNetDisassembler;

[ContextAction(
    GroupType = typeof(CSharpContextActions),
    ResourceType = typeof(Strings),
    NameResourceName = nameof(Strings.ShowAsmViewerContextAction_Name),
    DescriptionResourceName = nameof(Strings.ShowAsmViewerContextAction_Description),
    Priority = -10)]
public class ShowAsmViewerContextAction : ContextActionBase
{
    private static readonly InvisibleAnchor Anchor =
        BulbMenuAnchors.PermanentRoslynItems.CreateNext(separate: true);

    private readonly ICSharpContextActionDataProvider _dataProvider;

    public ShowAsmViewerContextAction(ICSharpContextActionDataProvider dataProvider)
    {
        _dataProvider = dataProvider;
    }

    public override string Text => Strings.ShowAsmViewerContextAction_Text;

    protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress)
    {
        return _ =>
        {
            var model = solution.GetProtocolSolution().GetAsmViewerModel();
            model.Show();

            var usageCollector = solution.GetComponent<AsmViewerUsageCollector>();
            usageCollector.LogContextActionInvoked();
        };
    }

    public override bool IsAvailable(IUserDataHolder cache)
    {
        var selectedElement = _dataProvider.GetSelectedElement<ITreeNode>();
        if (selectedElement == null)
            return false;

        return JitDisasmTargetUtils.FindValidDeclaration(selectedElement) != null;
    }

    public override IEnumerable<IntentionAction> CreateBulbItems()
    {
        return this.ToContextActionIntentions(Anchor, LocalHistoryThemedIcons.Diff.Id);
    }
}
