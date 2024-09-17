using System;
using System.Threading;
using JetBrains.Application.Progress;
using JetBrains.Platform.MsBuildTask.Utils;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.ContextActions;
using JetBrains.ReSharper.Feature.Services.CSharp.ContextActions;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.DeclaredElements;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Refactorings.Properties;
using JetBrains.ReSharper.UnitTestFramework.Common.Extensions;
using JetBrains.TextControl;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

[ContextAction(
    Group = CSharpContextActions.GroupID,
    ResourceType = typeof(Resources),
    NameResourceName = nameof(Resources.JitAsmViewerContextActionName),
    DescriptionResourceName = nameof(Resources.JitAsmViewerContextActionDescription),
    Priority = -10)]
public class ContextAction: ContextActionBase
{
    public ContextAction(ICSharpContextActionDataProvider provider) 
    {
        myProvider = provider;
    }
    
    private readonly ICSharpContextActionDataProvider myProvider;
    
    public override async void Execute(ISolution solution, ITextControl textControl)
    {
        if (myProvider.SelectedElement is not { } selectedItem ||
            (!Validate(selectedItem) && !Validate(selectedItem.Parent)) || selectedItem.Parent is not IDeclaration declaration)
        {
            return;
        }

        var target = GetTarget(declaration.DeclaredElement);
        var tempConfig = new JitDisasmConfiguration() { };
        var result = await new JitCodegenProvider(myProvider.Project).GetJitCodegen(target, tempConfig, textControl.Lifetime);    
    }

    protected override Action<ITextControl> ExecutePsiTransaction(ISolution solution, IProgressIndicator progress)
    {
        return x => {};
    }

    public override string Text { get; } = Resources.JitAsmViewerContextActionText;
    
    public override bool IsAvailable(IUserDataHolder cache)
    {
        return myProvider.SelectedElement is {} selectedItem && (Validate(selectedItem) || Validate(selectedItem.Parent));
    }

    private static bool Validate(ITreeNode node)
    {
        return node is ICSharpFunctionDeclaration
            or IClassDeclaration
            or IStructDeclaration
            or IRecordDeclaration
            or ILocalFunctionDeclaration
            or IConstructorDeclaration
            or IPropertyDeclaration
            or IOperatorDeclaration
            or IMethodDeclaration;
    }

   
    private DisasmTarget GetTarget(IDeclaredElement declaredElement)
    {
        string target;
        string hostType;
        string methodName;

        string prefix = "";
        ITypeElement containingType = declaredElement as ITypeElement ?? (declaredElement as ITypeMember)?.ContainingType;
        if (containingType is null)
            throw new Exception($"ContainingType is null for {declaredElement}");
        
        // match all for nested types
        if (containingType.GetContainingType() is not null)
            prefix = "*";
        else if (containingType.GetContainingNamespace() is { } containingNamespace && !string.IsNullOrWhiteSpace(containingNamespace.QualifiedName))
            prefix = containingNamespace.QualifiedName + ".";

        prefix += containingType.ShortName;

        switch (declaredElement)
        {
            case ILocalFunction:
                // hack for mangled names
                target = "*" + declaredElement.ShortName + "*";
                methodName = "*";
                hostType = containingType.ShortName;
                break;
            case IConstructor:
                target = prefix + ":.ctor";
                methodName = "*";
                hostType = containingType.ShortName;
                break;
            case IFunction:
                target = prefix + ":" + declaredElement.ShortName;
                methodName = declaredElement.ShortName;
                hostType = containingType.ShortName;
                break;
            case IProperty:
                target = prefix + ":get_" + declaredElement.ShortName + " " + prefix + ":set_" + declaredElement.ShortName;
                hostType = containingType.ShortName;
                methodName = declaredElement.ShortName;
                break;
            default:
                // the whole class
                target = prefix + ":*";
                hostType = declaredElement.ToString();
                methodName = "*";
                break;
        }

        return new DisasmTarget(target, hostType, methodName);
    }
}