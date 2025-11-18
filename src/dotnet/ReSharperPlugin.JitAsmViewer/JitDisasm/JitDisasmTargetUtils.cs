using System;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp.DeclaredElements;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.ReSharper.Psi.Tree;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public static class JitDisasmTargetUtils
{
    public static DisasmTarget GetTarget(IDeclaredElement declaredElement)
    {
        string target;
        string hostType;
        string methodName;

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
            case IFunction:
                target = prefix + ":" + declaredElement.ShortName;
                methodName = declaredElement.ShortName;
                hostType = containingType.ShortName;
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

        return new DisasmTarget(target, hostType, methodName);
    }
    
    public static bool ValidateTreeNodeForDisasm(ITreeNode node)
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