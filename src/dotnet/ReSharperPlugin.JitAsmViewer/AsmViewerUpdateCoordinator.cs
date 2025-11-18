using System;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi;
using JetBrains.Rider.Model;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using Result = JetBrains.Core.Result;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmViewerUpdateCoordinator
{
    private readonly SequentialLifetimes _updateLifetimes;
    private readonly AsmMethodLocator _methodLocator;
    private readonly AsmCompilationService _compilationService;
    private readonly AsmViewerModel _model;
    private readonly object _lock = new();

    private (string SourceFilePath, string MethodId, long? DocumentStamp) _lastState;
    private Result<string, Error>? _lastResult;

    public AsmViewerUpdateCoordinator(
        Lifetime lifetime,
        ISolution solution,
        AsmMethodLocator methodLocator,
        AsmCompilationService compilationService)
    {
        _updateLifetimes = new SequentialLifetimes(lifetime);
        _methodLocator = methodLocator;
        _compilationService = compilationService;
        _model = solution.GetProtocolSolution().GetAsmViewerModel();
    }

    public Task<Result<string, Error>> CompileWithDebouncingAsync(string sourceFilePath, int caretOffset)
    {
        var updateLifetime = _updateLifetimes.Next();
        return Task.Run(() => UpdateAsmAsync(sourceFilePath, caretOffset, updateLifetime), updateLifetime);
    }

    private async Task<Result<string, Error>> UpdateAsmAsync(string sourceFilePath, int caretOffset, Lifetime lifetime)
    {
        try
        {
            var declarationResult = _methodLocator.FindDeclarationAt(sourceFilePath, caretOffset);
            if (!declarationResult.Succeed)
                return Result.FailWithValue(declarationResult.FailValue);

            if (lifetime.IsNotAlive)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));

            var declaration = declarationResult.Value;
            var declaredElement = declaration.DeclaredElement;
            if (declaredElement == null)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.InvalidCaretPosition));

            var target = JitDisasmTargetUtils.GetTarget(declaredElement);
            var currentState = (sourceFilePath, target.Target, _model.DocumentModificationStamp.Value);

            lock (_lock)
            {
                if (currentState == _lastState && _lastResult.HasValue)
                    return _lastResult.Value;

                _lastState = currentState;
            }

            var configuration = JitDisasmConfigurationFactory.Create(_model);

            var compilationResult = await _compilationService.CompileAsync(
                declaration,
                configuration,
                lifetime);

            if (lifetime.IsNotAlive)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));

            Result<string, Error> result = compilationResult.Succeed
                ? Result.Success(compilationResult.Value)
                : Result.FailWithValue(compilationResult.FailValue);

            lock (_lock)
            {
                _lastResult = result;
            }

            return result;
        }
        catch (Exception ex)
        {
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError, ex.Message));
        }
    }
}
