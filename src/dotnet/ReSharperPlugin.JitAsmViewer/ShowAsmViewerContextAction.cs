using System;
using System.Collections.Generic;
using JetBrains.Application.Progress;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Bulbs;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Feature.Services.Intentions;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Model;
using JetBrains.TextControl;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

[ContextAction(
    GroupType = typeof(CSharpContextActions),
    Name = "Show JIT ASM Viewer",
    Description = "Opens the JIT ASM Viewer window to display JIT-compiled assembly code for the selected method, property, or constructor",
    Priority = -10)]
public class ShowAsmViewerContextAction : ContextActionBase
{
    private readonly ICSharpContextActionDataProvider _dataProvider;

    public ShowAsmViewerContextAction(ICSharpContextActionDataProvider dataProvider)
    {
        _dataProvider = dataProvider;
    }

    public override string Text => "Show JIT ASM";

    protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress)
    {
        return textControl =>
        {
            var model = solution.GetProtocolSolution().GetAsmViewerModel();
            model.Show();

            var usageCollector = solution.GetComponent<AsmViewerUsageCollector>();
            usageCollector.LogContextActionInvoked();
        };
    }

    public override bool IsAvailable(IUserDataHolder cache)
    {
        var declaration = GetDeclarationAtCaret();
        return declaration != null && JitDisasmTargetUtils.ValidateTreeNodeForDisasm(declaration);
    }

    private IDeclaration GetDeclarationAtCaret()
    {
        var selectedElement = _dataProvider.GetSelectedElement<ITreeNode>();
        if (selectedElement == null)
            return null;

        var declaration = selectedElement.GetContainingNode<IDeclaration>();
        return declaration;
    }

    public override IEnumerable<IntentionAction> CreateBulbItems()
    {
        return this.ToContextActionIntentions();
    }
}
