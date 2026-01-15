using System;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Application.Threading;
using JetBrains.DataFlow;
using JetBrains.Lifetimes;
using JetBrains.Platform.RdFramework.Util;
using JetBrains.DocumentManagers;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Navigation;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.ReSharper.Psi.Caches;
using JetBrains.Rider.Model;
using JetBrains.TextControl;
using JetBrains.TextControl.CodeWithMe;
using JetBrains.Threading;
using JetBrains.Util;
using ReSharperPlugin.DotNetDisassembler.JitDisasmAdapters;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.DotNetDisassembler;

[SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
public class AsmViewerHost
{
    private readonly ILogger _logger = GetLogger<AsmViewerHost>();

    private readonly DocumentManager _documentManager;
    private readonly IFileLocationsBlacklist _fileLocationsBlacklist;
    private readonly IShellLocks _shellLocks;
    private readonly AsmViewerUpdateCoordinator _updateCoordinator;
    private readonly AsmViewerUsageCollector _usageCollector;
    private readonly AsmViewerModel _model;

    private volatile ITextControl _currentTextControl;

    public AsmViewerHost(
        Lifetime lifetime,
        ISolution solution,
        DocumentManager documentManager,
        IFileLocationsBlacklist fileLocationsBlacklist,
        IShellLocks shellLocks,
        ITextControlManager textControlManager,
        IPsiCachesState psiCachesState,
        AsmViewerUpdateCoordinator updateCoordinator,
        AsmViewerUsageCollector usageCollector)
    {
        _documentManager = documentManager;
        _fileLocationsBlacklist = fileLocationsBlacklist;
        _shellLocks = shellLocks;
        _updateCoordinator = updateCoordinator;
        _usageCollector = usageCollector;
        _model = solution.GetProtocolSolution().GetAsmViewerModel();

        SubscribeToVisibility(lifetime, psiCachesState, textControlManager);

        _logger.Info("AsmViewerHost initialized");
    }

    private void SubscribeToVisibility(Lifetime lifetime, IPsiCachesState psiCachesState, ITextControlManager textControlManager)
    {
        var isVisible = new Property<bool>("AsmViewerHost.IsVisible");
        _model.IsVisible.FlowInto(lifetime, isVisible);

        isVisible.WhenTrue(lifetime, visibleLifetime =>
        {
            _logger.Verbose("ASM Viewer tool window visible");

            var compilationLifetimes = new SequentialLifetimes(visibleLifetime);

            var compilationTrigger = _shellLocks.CreateGroupingEvent(
                visibleLifetime,
                "AsmViewerHost.CompilationTrigger",
                TimeSpan.FromMilliseconds(300),
                () => RequestCompilationAsync(compilationLifetimes).NoAwait());

            SubscribeToConfigurationChanges(visibleLifetime, compilationTrigger);
            SubscribeToRecompileRequests(visibleLifetime, compilationTrigger);
            SubscribeToForceRecompileRequests(visibleLifetime, compilationTrigger);

            psiCachesState.IsInitialUpdateFinished.WhenTrue(visibleLifetime, psiReadyLifetime =>
            {
                SubscribeToTextControlChanges(psiReadyLifetime, textControlManager, compilationTrigger);
            });
        });
        
        isVisible.WhenFalse(lifetime, _ =>
        {
            _logger.Verbose("ASM Viewer tool window hidden");
            _currentTextControl = null;
        });
    }

    private void SubscribeToConfigurationChanges(Lifetime lifetime, GroupingEvent compilationTrigger)
    {
        _model.Configuration.Advise(lifetime, _ =>
        {
            _logger.Verbose("Configuration changed");
            compilationTrigger.FireIncoming();
        });
    }

    private void SubscribeToRecompileRequests(Lifetime lifetime, GroupingEvent compilationTrigger)
    {
        _model.Recompile.Advise(lifetime, _ =>
        {
            _logger.Verbose("Recompile requested");
            compilationTrigger.FireIncoming();
        });
    }

    private void SubscribeToForceRecompileRequests(Lifetime lifetime, GroupingEvent compilationTrigger)
    {
        _model.ForceRecompile.Advise(lifetime, _ =>
        {
            _logger.Verbose("Force recompile requested, invalidating cache");
            _updateCoordinator.InvalidateCache();
            compilationTrigger.FireIncoming();
        });
    }

    private void SubscribeToTextControlChanges(Lifetime lifetime, ITextControlManager textControlManager, GroupingEvent compilationTrigger)
    {
        var caretSubscriptions = new SequentialLifetimes(lifetime);

        textControlManager.LastFocusedTextControlPerClient.ForEachValue_Host(lifetime, (_, textControl) =>
        {
            if (textControl == null)
            {
                _logger.Verbose("No focused text control");
                _currentTextControl = null;
                caretSubscriptions.TerminateCurrent();
                _model.SendResult(new CompilationResult(null, new ErrorInfo(ErrorCode.SourceFileNotFound, null)));
                return;
            }

            // IL Viewer, ASM Viewer and similar tools open read-only editors with synchronized carets.
            // We skip these and keep tracking the source editor - caret sync ensures our subscription still works.
            var projectFile = _documentManager.TryGetProjectFile(textControl.Document);
            if (projectFile == null || _fileLocationsBlacklist.Contains(projectFile.Location))
            {
                _logger.Verbose("Ignoring text control (virtual document or blacklisted): {0}", textControl.Document.Moniker);
                return;
            }

            if (textControl == _currentTextControl)
            {
                _logger.Verbose("Same source text control, skipping resubscription");
                return;
            }

            _currentTextControl = textControl;

            _logger.Verbose("Text control focused: {0}", textControl.Document.Moniker);
            compilationTrigger.FireIncoming();

            SubscribeToCaretChanges(caretSubscriptions, textControl, compilationTrigger);
        });
    }

    private static void SubscribeToCaretChanges(SequentialLifetimes subscriptions, ITextControl textControl, GroupingEvent compilationTrigger)
    {
        var lifetime = Lifetime.Intersect(subscriptions.Next(), textControl.Lifetime);
        textControl.Caret.Position.Change.Advise(lifetime, _ => compilationTrigger.FireIncoming());
    }

    private async Task RequestCompilationAsync(SequentialLifetimes compilationLifetimes)
    {
        var configuration = _model.Configuration.Maybe;
        if (!configuration.HasValue)
        {
            _logger.Verbose("Configuration not initialized, skipping");
            return;
        }

        var textControl = _currentTextControl;
        if (textControl == null)
        {
            _logger.Verbose("No text control, skipping");
            return;
        }

        var compilationLifetime = compilationLifetimes.Next();
        _logger.Info("Starting compilation");

        var result = await _updateCoordinator.CompileAsync(textControl, configuration.Value, compilationLifetime);

        if (!compilationLifetime.IsAlive || result.FailValue is { Code: AsmViewerErrorCode.UpdateCancelled })
        {
            _logger.Verbose("Compilation cancelled");
            return;
        }

        if (!result.Succeed)
        {
            _logger.Warn("Compilation failed: {0} - {1}", result.FailValue.Code, result.FailValue.Details);
            _usageCollector.LogError(result.FailValue.Code);
            _model.SendResult(new CompilationResult(null, new ErrorInfo(result.FailValue.Code.ToRdErrorCode(), result.FailValue.Details)));
            return;
        }

        _logger.Info("Compilation succeeded, result length: {0}", result.Value.Length);
        _usageCollector.LogDisassemblySucceeded();
        _model.SendResult(new CompilationResult(result.Value, null));
    }
}
