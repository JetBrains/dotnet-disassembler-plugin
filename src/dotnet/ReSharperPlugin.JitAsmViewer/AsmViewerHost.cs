using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;
using JetBrains.Util;
using JetBrains.Util.Logging;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using Result = JetBrains.Core.Result;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
public class AsmViewerHost
{
    private readonly ILogger _logger = Logger.GetLogger(typeof(AsmViewerHost));
    private readonly AsmViewerModel _model;
    private readonly IThreading _threading;
    private readonly AsmViewerUpdateCoordinator _updateCoordinator;
    private readonly AsmViewerUsageCollector _usageCollector;

    public AsmViewerHost(
        Lifetime lifetime,
        ISolution solution,
        IThreading threading,
        AsmViewerUpdateCoordinator updateCoordinator,
        AsmViewerUsageCollector usageCollector)
    {
        _logger.Info("Initializing AsmViewerHost for solution: {0}", solution.Name);
        _model = solution.GetProtocolSolution().GetAsmViewerModel();
        _threading = threading;
        _updateCoordinator = updateCoordinator;
        _usageCollector = usageCollector;

        SubscribeToModelChanges(lifetime);
        _logger.Info("AsmViewerHost initialized successfully");
    }

    private void RefreshAsmContent(Lifetime lifetime)
    {
        if (!_model.IsVisible.Maybe.HasValue || !_model.IsVisible.Value)
        {
            _logger.Verbose("Skipping refresh - tool window not visible");
            return;
        }

        var caretPosition = _model.CaretPosition.Maybe.ValueOrDefault;
        if (caretPosition == null)
        {
            _logger.Verbose("Skipping refresh - no caret position");
            return;
        }

        _logger.Info("Refreshing ASM content for file: {0}, offset: {1}", caretPosition.FilePath, caretPosition.Offset);

        _ = _updateCoordinator
            .CompileWithDebouncingAsync(caretPosition)
            .ContinueWith(task =>
            {
                if (lifetime.IsNotAlive)
                {
                    _logger.Verbose("Lifetime expired, skipping model update");
                    return;
                }

                if (task.IsFaulted)
                {
                    _logger.Error(task.Exception, "Unexpected error during compilation");
                    var errorResult = Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError,
                        task.Exception?.GetBaseException().Message ?? "Unknown error"));

                    _threading.ExecuteOrQueueEx(lifetime, "AsmViewer.UpdateModel", () =>
                    {
                        UpdateModel(errorResult);
                    });
                    return;
                }

                var result = task.Result;

                _threading.ExecuteOrQueueEx(lifetime, "AsmViewer.UpdateModel", () =>
                {
                    UpdateModel(result);
                });
            }, lifetime, TaskContinuationOptions.None, TaskScheduler.Default);
    }

    private void UpdateModel(Result<string, Error> result)
    {
        if (result.Succeed)
        {
            _logger.Info("Disassembly succeeded, content length: {0}", result.Value.Length);
            _model.CompilationResult.Value = new CompilationResult(result.Value, null);

            _usageCollector.LogDisassemblySucceeded();
        }
        else
        {
            var error = result.FailValue;

            if (error.Code == AsmViewerErrorCode.UpdateCancelled)
            {
                _logger.Verbose("Update cancelled, keeping current content");
                return;
            }

            _logger.Warn("Disassembly failed - Code: {0}, Details: {1}", error.Code, error.Details);

            var errorInfo = new ErrorInfo(error.Code.ToString(), error.Details);
            _model.CompilationResult.Value = new CompilationResult(null, errorInfo);

            _usageCollector.LogError(error.Code);
        }
    }
    
    private void SubscribeToModelChanges(Lifetime lifetime)
    {
        _model.IsVisible.Advise(lifetime, _ => RefreshAsmContent(lifetime));
        _model.CaretPosition.Advise(lifetime, _ => RefreshAsmContent(lifetime));
        _model.Configuration.Advise(lifetime, config =>
        {
            RefreshAsmContent(lifetime);

            var configuration = JitDisasmConfigurationFactory.Create(config);
            _usageCollector.LogConfigurationSaved(configuration);
        });
    }
}
