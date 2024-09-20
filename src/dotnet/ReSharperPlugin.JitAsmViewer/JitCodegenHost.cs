using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.DataContext;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi.DataContext;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.Rider.Model;
using JetBrains.TextControl;
using JetBrains.TextControl.CodeWithMe;
using JetBrains.TextControl.DataContext;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using Result = JetBrains.Core.Result;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.ContainerAsyncAnyThread)] //TODO change to DemandAnyThreadSafe
public class JitCodegenHost
{
    private readonly ITextControlManager _textControlManager;
    private readonly DataContexts _dataContexts;
    private readonly ISolution _solution;

    public JitCodegenHost(Lifetime lifetime, ITextControlManager textControlManager,
        DataContexts dataContexts,
        ISolution solution)
    {
        _textControlManager = textControlManager;
        _dataContexts = dataContexts;
        _solution = solution;
        var model = solution.GetProtocolSolution().GetJitAsmProviderModel();
        model.GetJitCodegenForSelectedElement.Set(GetJitCodegenForCurrentSelectedElement);
    }

    private async Task<string> GetJitCodegenForCurrentSelectedElement(Lifetime lifetime, Unit _)
    {
        var textControl = _textControlManager.LastFocusedTextControlPerClient.ForCurrentClient();
        var dataContext = textControl.ToDataContext()(lifetime, _dataContexts);
        var elementUnderCaret = dataContext.GetData(PsiDataConstants.SELECTED_TREE_NODES)?.FirstOrDefault();
        if (elementUnderCaret is not { } selectedItem ||
            !JitDisasmTargetUtils.ValidateTreeNodeForDisasm(selectedItem.Parent) ||
            selectedItem.Parent is not IDeclaration declaration)
            return null;

        var result = (await GetJitCodegenForDeclaration(declaration, lifetime)).Value.Result;
        return result;
    }

    private async Task<Result<JitCodeGenResult>> GetJitCodegenForDeclaration(IDeclaration declaration,
        CancellationToken token)
    {
        var target = JitDisasmTargetUtils.GetTarget(declaration.DeclaredElement);
        var tempConfig = new JitDisasmConfiguration();
        return await new JitCodegenProvider(declaration.GetProject()).GetJitCodegen(target, tempConfig, token);
    }
}