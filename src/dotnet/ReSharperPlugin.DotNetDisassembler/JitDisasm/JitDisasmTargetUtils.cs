using System;
using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.DeclaredElements;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.DataContext;
using JetBrains.ReSharper.Psi.Tree;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public static class JitDisasmTargetUtils
{
    [CanBeNull]
    public static IDeclaration FindValidDeclaration(PsiEditorView editorView)
    {
        var psiView = editorView.DefaultSourceFile.ViewDominant();
        var selectedNode = psiView.GetSelectedTreeNode<ITreeNode>();
        return selectedNode?.GetContainingNode<IDeclaration>(returnThis: true, predicate: IsValidDisasmTarget);
    }

    [CanBeNull]
    public static IDeclaration FindValidDeclaration([CanBeNull] ITreeNode treeNode)
    {
        return treeNode?.GetContainingNode<IDeclaration>(returnThis: true, predicate: IsValidDisasmTarget);
    }

    public static DisasmTarget GetTarget(IDeclaredElement declaredElement)
    {
        string target;
        string hostType;
        string methodName;
        bool isGenericMethod = false;

        string prefix = "";
        ITypeElement containingType =
            declaredElement as ITypeElement ?? (declaredElement as ITypeMember)?.ContainingType ?? (declaredElement as ITypeOwnerDeclaration)?.GetContainingTypeElement();
        if (containingType is null)
            throw new Exception($"Unable to determine containing type for '{declaredElement.ShortName}' (type: {declaredElement.GetType().Name}). Make sure the method is inside a .NET Core/6+ project.");

        // match all for nested types
        if (containingType.GetContainingType() is not null)
            prefix = "*";
        else if (containingType.GetContainingNamespace() is { } containingNamespace &&
                 !string.IsNullOrWhiteSpace(containingNamespace.QualifiedName))
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
            case IFunction function:
                target = prefix + ":" + declaredElement.ShortName;
                methodName = declaredElement.ShortName;
                hostType = containingType.ShortName;
                isGenericMethod = function is ITypeParametersOwner { TypeParameters.Count: > 0 };
                break;
            case IProperty:
                target = prefix + ":get_" + declaredElement.ShortName + " " + prefix + ":set_" +
                         declaredElement.ShortName;
                hostType = containingType.ShortName;
                methodName = declaredElement.ShortName;
                break;
            default:
                // the whole class
                target = prefix + ":*";
                hostType = containingType.ShortName;
                methodName = "*";
                break;
        }

        return new DisasmTarget(target, hostType, methodName, isGenericMethod);
    }
    
    private static bool IsValidDisasmTarget(ITreeNode node)
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
}