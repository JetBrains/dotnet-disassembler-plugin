using System;
using JetBrains.UsageStatistics.FUS.Collectors;
using JetBrains.UsageStatistics.FUS.EventLog;
using JetBrains.UsageStatistics.FUS.EventLog.Events;
using JetBrains.UsageStatistics.FUS.EventLog.Fus;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer;

[CounterUsagesCollector]
public class AsmViewerUsageCollector : CounterUsagesCollector
{
    private static readonly ILogger Logger = GetLogger(typeof(AsmViewerUsageCollector));

    private readonly EventLogGroup _group;

    private readonly EventId _contextActionInvoked;
    private readonly VarargEventId _projectInfoCollected;
    private readonly EventId1<AsmViewerErrorCode> _errorOccurred;
    private readonly EventId _disassemblySucceeded;
    private readonly VarargEventId _configurationSaved;

    private readonly StringEventField _sdkTypeField;
    private readonly StringEventField _targetFrameworkField;

    private readonly BooleanEventField _showAsmCommentsField;
    private readonly BooleanEventField _useTieredJitField;
    private readonly BooleanEventField _usePgoField;
    private readonly BooleanEventField _diffableField;
    private readonly BooleanEventField _runAppModeField;
    private readonly BooleanEventField _useNoRestoreField;
    private readonly BooleanEventField _useUnloadableContextField;
    private readonly BooleanEventField _dontGuessTfmField;
    private readonly StringEventField _jitCompilerField;
    private readonly BooleanEventField _useDotnetPublishField;

    public AsmViewerUsageCollector(FeatureUsageLogger featureUsageLogger)
    {
        _group = new EventLogGroup("jit.asm.viewer.backend", "JIT ASM Viewer Backend", 1, featureUsageLogger);

        _contextActionInvoked = _group.RegisterEvent("context_action.invoked", "Context action invoked");

        _disassemblySucceeded = _group.RegisterEvent("disassembly.succeeded", "Disassembly succeeded");

        _sdkTypeField = EventFields.String("sdk_type", "Project SDK type", Array.Empty<string>());
        _targetFrameworkField = EventFields.String("target_framework", "Target framework", Array.Empty<string>());
        _projectInfoCollected = _group.RegisterVarargEvent(
            "project.info",
            "Project information",
            _sdkTypeField,
            _targetFrameworkField);

        var errorCodeField = EventFields.Enum<AsmViewerErrorCode>("error_code", "Error code");
        _errorOccurred = _group.RegisterEvent(
            "error.occurred",
            "Error occurred",
            errorCodeField);

        _showAsmCommentsField = EventFields.Boolean("show_asm_comments", "Show ASM comments");
        _useTieredJitField = EventFields.Boolean("use_tiered_jit", "Use tiered JIT");
        _usePgoField = EventFields.Boolean("use_pgo", "Use PGO");
        _diffableField = EventFields.Boolean("diffable", "Diffable mode");
        _runAppModeField = EventFields.Boolean("run_app_mode", "Run app mode");
        _useNoRestoreField = EventFields.Boolean("use_no_restore", "Use no restore");
        _useUnloadableContextField = EventFields.Boolean("use_unloadable_context", "Use unloadable context");
        _dontGuessTfmField = EventFields.Boolean("dont_guess_tfm", "Don't guess TFM");
        _jitCompilerField = EventFields.String("jit_compiler", "JIT compiler", Array.Empty<string>());
        _useDotnetPublishField = EventFields.Boolean("use_dotnet_publish", "Use dotnet publish");
        _configurationSaved = _group.RegisterVarargEvent(
            "configuration.changed",
            "Configuration changed",
            _showAsmCommentsField,
            _useTieredJitField,
            _usePgoField,
            _diffableField,
            _runAppModeField,
            _useNoRestoreField,
            _useUnloadableContextField,
            _dontGuessTfmField,
            _jitCompilerField,
            _useDotnetPublishField);
    }

    public override EventLogGroup GetGroup()
    {
        return _group;
    }

    public void LogContextActionInvoked()
    {
        _contextActionInvoked.Log();
    }

    public void LogProjectInfo(JitDisasmProjectContext projectContext)
    {
        try
        {
            _projectInfoCollected.Log(
                _sdkTypeField.With(projectContext.Sdk),
                _targetFrameworkField.With(projectContext.Tfm?.UniqueString)
            );
        }
        catch (Exception ex)
        {
            Logger.LogExceptionSilently(ex);
            // Ignore errors during error logging
        }
    }

    public void LogError(AsmViewerErrorCode errorCode)
    {
        try
        {
            _errorOccurred.Log(errorCode);
        }
        catch (Exception ex)
        {
            Logger.LogExceptionSilently(ex);
            // Ignore errors during error logging
        }
    }

    public void LogDisassemblySucceeded()
    {
        try
        {
            _disassemblySucceeded.Log();
        }
        catch (Exception ex)
        {
            Logger.LogExceptionSilently(ex);
            // Ignore errors during logging
        }
    }

    public void LogConfigurationSaved(JitDisasmConfiguration config)
    {
        try
        {
            _configurationSaved.Log(
                _showAsmCommentsField.With(config.ShowAsmComments),
                _useTieredJitField.With(config.UseTieredJit),
                _usePgoField.With(config.UsePgo),
                _diffableField.With(config.Diffable),
                _runAppModeField.With(config.RunAppMode),
                _useNoRestoreField.With(config.UseNoRestoreFlag),
                _useUnloadableContextField.With(config.UseUnloadableContext),
                _dontGuessTfmField.With(config.DontGuessTfm),
                _jitCompilerField.With(config.SelectedCustomJit),
                _useDotnetPublishField.With(config.UseDotnetPublishForReload));
        }
        catch (Exception ex)
        {
            Logger.LogExceptionSilently(ex);
            // Ignore errors during logging
        }
    }
}
