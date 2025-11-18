using System.Linq;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.DocumentModel;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using VirtualFileSystemPath = JetBrains.Util.VirtualFileSystemPath;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmMethodLocator
{
    private static readonly ILogger Logger = JetBrains.Util.Logging.Logger.GetLogger(typeof(AsmMethodLocator));
    private readonly ISolution _solution;
    private readonly IPsiCachesState _psiCachesState;

    public AsmMethodLocator(ISolution solution, IPsiCachesState psiCachesState)
    {
        _solution = solution;
        _psiCachesState = psiCachesState;
    }

    public Result<IDeclaration, Error> FindDeclarationAt(string sourceFilePath, int caretOffset)
    {
        if (!_psiCachesState.IsInitialUpdateFinished.Value)
        {
            Logger.Verbose("FindDeclarationAt: PSI caches not ready yet");
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
        }

        return _solution.Locks.ExecuteWithReadLock<Result<IDeclaration, Error>>(() =>
        {
            var virtualPath = VirtualFileSystemPath.Parse(sourceFilePath, InteractionContext.SolutionContext);

            var projectFile = _solution.FindProjectItemsByLocation(virtualPath)
                .OfType<IProjectFile>()
                .FirstOrDefault();

            if (projectFile == null)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.SourceFileNotFound));

            var psiSourceFile = projectFile.ToSourceFile();
            if (psiSourceFile == null)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.PsiSourceFileUnavailable));

            var psiFile = psiSourceFile.GetPsiFiles<CSharpLanguage>().FirstOrDefault();
            var document = psiSourceFile.Document;

            if (psiFile == null || document == null)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UnsupportedLanguage));

            var element = psiFile.FindNodeAt(new DocumentOffset(document, caretOffset));
            if (element == null)
            {
                Logger.Verbose("FindDeclarationAt: No element found at offset {0}", caretOffset);
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));
            }

            Logger.Verbose("FindDeclarationAt: Element found - {0}", element.GetType().Name);

            var declaration = element.GetContainingNode<IDeclaration>(true);

            if (declaration == null || !JitDisasmTargetUtils.ValidateTreeNodeForDisasm(declaration))
            {
                Logger.Verbose("FindDeclarationAt: No valid declaration found at offset {0}, element: {1}",
                    caretOffset, element.GetType().Name);
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));
            }

            Logger.Info("FindDeclarationAt: Found declaration - {0}", declaration.GetType().Name);
            return Result.Success(declaration);
        });
    }
}
