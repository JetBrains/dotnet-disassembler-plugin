using System;
using System.Threading.Tasks;
using JetBrains.Annotations;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.Util;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.ReSharper.Resources.Shell;
using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using Result = JetBrains.Core.Result;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmViewerUpdateCoordinator(
    ISolution solution,
    AsmMethodLocator methodLocator,
    AsmCompilationService compilationService)
{
    private static readonly ILogger Logger = GetLogger(typeof(AsmViewerUpdateCoordinator));

    private readonly AsmViewerModel _model = solution.GetProtocolSolution().GetAsmViewerModel();
    private readonly object _cacheLock = new();

    private int _cacheVersion;
    [CanBeNull] private CacheEntry _cache;

    public async Task<Result<string, Error>> CompileAsync(CompileRequest request, Lifetime lifetime)
    {
        try
        {
            var caretPosition = request.CaretPosition;
            var declarationResult = methodLocator.FindDeclarationAt(caretPosition.FilePath, caretPosition.Offset);
            if (!declarationResult.Succeed)
                return Result.FailWithValue(declarationResult.FailValue);

            if (lifetime.IsNotAlive)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));

            var declaration = declarationResult.Value;
            var declaredElement = declaration.DeclaredElement;
            if (declaredElement == null)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));

            var target = JitDisasmTargetUtils.GetTarget(declaredElement);
            var configuration = JitDisasmConfigurationFactory.Create(request.Configuration);
            var filePath = caretPosition.FilePath;
            var methodId = target.Target;
            var documentStamp = caretPosition.DocumentModificationStamp;

            var project = declaration.GetProject();
            var projectContext = project != null ? JitDisasmProjectContextFactory.Create(project) : null;

            int myCacheVersion;
            lock (_cacheLock)
            {
                if (_cache is { } entry && entry.StateEquals(filePath, methodId, documentStamp, configuration, projectContext))
                {
                    Logger.Verbose("Cache hit, returning cached result (version: {0})", entry.Version);
                    return entry.Result;
                }

                myCacheVersion = ++_cacheVersion;
            }

            return await lifetime.StartMainWriteAsync(async () =>
            {
                _model.IsLoading.Value = true;
                try
                {
                    var compilationResult = await compilationService.CompileAsync(
                        declaration,
                        configuration,
                        projectContext,
                        lifetime);

                    if (lifetime.IsNotAlive)
                    {
                        Logger.Info("Lifetime is not alive, returning cancelled");
                        return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
                    }

                    lock (_cacheLock)
                    {
                        if (_cacheVersion != myCacheVersion)
                            return compilationResult;

                        _cache = new CacheEntry(myCacheVersion, filePath, methodId, documentStamp, configuration, projectContext, compilationResult);
                        Logger.Verbose("Cache updated (version: {0}), success: {1}", myCacheVersion, compilationResult.Succeed);
                    }

                    return compilationResult;
                }
                finally
                {
                    _model.IsLoading.Value = false;
                }
            });
        }
        catch (OperationCanceledException)
        {
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
        }
        catch (Exception ex)
        {
            Logger.LogException(ex);
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError, ex.Message));
        }
    }
    
    private sealed record CacheEntry(int Version, string SourceFilePath, string MethodId, long DocumentStamp,
        JitDisasmConfiguration Configuration, JitDisasmProjectContext ProjectContext, Result<string, Error> Result)
    {
        public bool StateEquals(string filePath, string methodId, long stamp, JitDisasmConfiguration config, JitDisasmProjectContext projectContext) =>
            SourceFilePath == filePath && MethodId == methodId && DocumentStamp == stamp && Configuration == config && ProjectContext == projectContext;
    }
}
