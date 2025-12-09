using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.ProjectModel;
using JetBrains.Rd;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.ReSharper.Psi.DataContext;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.TextControl;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer;

public sealed record DeclarationData(
    DisasmTarget Target,
    string FilePath,
    long FileStamp,
    JitDisasmProjectContext ProjectContext);

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmMethodLocator(
    ISolution solution,
    ITextControlManager textControlManager,
    IPsiCachesState psiCachesState)
{
    private static readonly ILogger Logger = GetLogger<AsmMethodLocator>();
    
    public Result<DeclarationData, Error> FindDeclarationAtCaret()
    {
        if (!psiCachesState.IsInitialUpdateFinished.Value)
        {
            Logger.Verbose("Waiting for PSI caches to initialize");
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
        }

        if (!textControlManager.LastFocusedTextControlPerClient.TryGetValue(ClientId.LocalId, out var textControl))
        {
            Logger.Verbose("No editor is currently focused");
            return Result.FailWithValue(new Error(AsmViewerErrorCode.SourceFileNotFound));
        }

        return solution.Locks.ExecuteWithReadLock<Result<DeclarationData, Error>>(() =>
        {
            var psiEditorView = new PsiEditorView(solution, textControl);

            var declaration = JitDisasmTargetUtils.FindValidDeclaration(psiEditorView);
            if (declaration == null)
            {
                Logger.Verbose("Caret is not positioned on a method or class declaration");
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));
            }

            var declaredElement = declaration.DeclaredElement;
            if (declaredElement == null)
            {
                Logger.Verbose("Declaration has no associated symbol (possibly incomplete code)");
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));
            }

            var sourceFile = declaration.GetSourceFile();
            if (sourceFile == null)
            {
                Logger.Verbose("Declaration is not associated with a source file");
                return Result.FailWithValue(new Error(AsmViewerErrorCode.PsiSourceFileUnavailable));
            }

            var location = sourceFile.GetLocation();
            if (location.IsEmpty)
            {
                Logger.Verbose("Source file '{0}' has no path on disk", sourceFile.Name);
                return Result.FailWithValue(new Error(AsmViewerErrorCode.SourceFileNotFound));
            }

            var project = declaration.GetProject();
            if (project == null)
            {
                Logger.Verbose("Source file '{0}' is not part of any project", location.FullPath);
                return Result.FailWithValue(new Error(AsmViewerErrorCode.SourceFileNotFound));
            }

            var target = JitDisasmTargetUtils.GetTarget(declaredElement);
            var filePath = location.FullPath;
            var fileStamp = location.FileModificationTimeUtc.Ticks;
            var projectContext = JitDisasmProjectContextFactory.Create(project);

            Logger.Info("Found target '{0}' in '{1}'", target.MemberFilter, filePath);
            return Result.Success(new DeclarationData(target, filePath, fileStamp, projectContext));
        });
    }
}
