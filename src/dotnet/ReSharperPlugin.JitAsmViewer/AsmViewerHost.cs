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

[SolutionComponent(Instantiation.ContainerAsyncAnyThreadUnsafe)]
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

        _threading.ExecuteOrQueueEx(lifetime, "AsmViewer.Initialize", () =>
        {
            _logger.Verbose("AsmViewer initialization started");
            AsmViewerModelInitializer.Initialize(_model);
            SubscribeToModelChanges(lifetime);
            SubscribeToStatistics(lifetime);
            _logger.Info("AsmViewerHost initialized successfully");
        });
    }

    private void SubscribeToModelChanges(Lifetime lifetime)
    {
        _model.AdviseRefreshTriggers(lifetime, () => RefreshAsmContent(lifetime));
    }

    private void RefreshAsmContent(Lifetime lifetime)
    {
        if (!_model.IsVisible.Maybe.HasValue || !_model.IsVisible.Value)
        {
            _logger.Verbose("Skipping refresh - tool window not visible");
            return;
        }

        var sourceFilePath = _model.SourceFilePath.Value;
        var caretOffset = _model.CaretOffset.Value;

        if (sourceFilePath == null || !caretOffset.HasValue)
        {
            _logger.Verbose("Skipping refresh - no source file or caret offset");
            return;
        }

        _logger.Info("Refreshing ASM content for file: {0}, offset: {1}", sourceFilePath, caretOffset.Value);

        _threading.ExecuteOrQueueEx(lifetime, "AsmViewer.SetLoading", () =>
        {
            _model.IsLoading.Value = true;
            _model.UnavailabilityReason.Value = null;
        });

        _ = _updateCoordinator
            .CompileWithDebouncingAsync(sourceFilePath, caretOffset.Value)
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
                        _model.IsLoading.Value = false;
                    });
                    return;
                }

                var result = task.Result;

                _threading.ExecuteOrQueueEx(lifetime, "AsmViewer.UpdateModel", () =>
                {
                    UpdateModel(result);
                    _model.IsLoading.Value = false;
                });
            }, lifetime, TaskContinuationOptions.None, TaskScheduler.Default);
    }

    private void UpdateModel(Result<string, Error> result)
    {
        if (result.Succeed)
        {
            _logger.Info("Disassembly succeeded, content length: {0}", result.Value.Length);
            _model.CurrentContent.Value = result.Value;
            _model.UnavailabilityReason.Value = null;
            _model.ErrorCode.Value = null;

            _usageCollector.LogDisassemblySucceeded();
        }
        else
        {
            var error = result.FailValue;
            _logger.Warn("Disassembly failed - Code: {0}, Message: {1}", error.Code, error.Message);

            _model.UnavailabilityReason.Value = error.Message;
            _model.ErrorCode.Value = error.Code.ToString();

            _usageCollector.LogError(error.Code);
        }
    }

    private void SubscribeToStatistics(Lifetime lifetime)
    {
        _model.AdviseRefreshTriggers(lifetime, () =>
        {
            if (_model.SourceFilePath.Maybe.HasValue)
            {
                LogCurrentConfiguration();
            }
        });
    }

    private void LogCurrentConfiguration()
    {
        var configuration = JitDisasmConfigurationFactory.Create(_model);
        _usageCollector.LogConfigurationSaved(configuration);
    }
}
