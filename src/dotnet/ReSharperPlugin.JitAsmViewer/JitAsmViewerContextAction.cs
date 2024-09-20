using System;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Progress;
using JetBrains.Core;
using JetBrains.IDE.UI;
using JetBrains.IDE.UI.Extensions;
using JetBrains.Lifetimes;
using JetBrains.Platform.MsBuildTask.Utils;
using JetBrains.ProjectModel;
using JetBrains.Rd.Base;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.DeclaredElements;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Refactorings.Properties;
using JetBrains.ReSharper.UnitTestFramework.Common.Extensions;
using JetBrains.Rider.Model.UIAutomation;
using JetBrains.TextControl;
using JetBrains.Threading;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

[ContextAction(
    Group = CSharpContextActions.GroupID,
    ResourceType = typeof(Resources),
    NameResourceName = nameof(Resources.JitAsmViewerContextActionName),
    DescriptionResourceName = nameof(Resources.JitAsmViewerContextActionDescription),
    Priority = -10)]
public class JitAsmViewerContextAction : ContextActionBase
{
    public JitAsmViewerContextAction(ICSharpContextActionDataProvider provider)
    {
        myProvider = provider;
    }

    private readonly ICSharpContextActionDataProvider myProvider;

    public override void Execute(ISolution solution, ITextControl textControl)
    {
        if (myProvider.SelectedElement is not { } selectedItem || !Validate(selectedItem.Parent) ||
            selectedItem.Parent is not IDeclaration declaration)
            return;
        
        ShowCodegen().NoAwait();
        
        async Task ShowCodegen()
        {
            var result = await GetJitCodegenForDeclaration(declaration, textControl.Lifetime);
            var dialogHost = solution.GetComponent<IDialogHost>();

            BeTextPanel textPanel = new BeTextPanel();
            textPanel.Text.Set(result.Value.Result);
            dialogHost.Show(getDialog: lt => BeControls
                .GetDialog(dialogContent: textPanel, title: Resources.JitAsmViewerContextActionName,
                    id: nameof(JitAsmViewerContextAction))
                .WithOkButton(lt), parentLifetime: Lifetime.Eternal);
        }
    }

    public async Task<Result<JitCodeGenResult>> GetJitCodegenForSelectedElement(CancellationToken token)
    {
        if (myProvider.SelectedElement is not { } selectedItem || !Validate(selectedItem.Parent) ||
            selectedItem.Parent is not IDeclaration declaration)
            return Result.Fail();
        
        return await GetJitCodegenForDeclaration(declaration, token);
    }
    
    private async Task<Result<JitCodeGenResult>> GetJitCodegenForDeclaration(IDeclaration declaration,
        CancellationToken token)
    {
        var target = JitDisasmTargetUtils.GetTarget(declaration.DeclaredElement);
        var tempConfig = new JitDisasmConfiguration();
        return await new JitCodegenProvider(myProvider.Project).GetJitCodegen(target, tempConfig, token);
    }

    protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress)
    {
        return x => { };
    }

    public override string Text { get; } = Resources.JitAsmViewerContextActionText;


    public override bool IsAvailable(IUserDataHolder cache)
    {
        return myProvider.SelectedElement is { } selectedItem &&
               (Validate(selectedItem) || Validate(selectedItem.Parent));
    }

    private static bool Validate(ITreeNode node)
    {
        return JitDisasmTargetUtils.ValidateTreeNodeForDisasm(node);
    }
}