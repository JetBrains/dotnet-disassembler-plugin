using System;
using JetBrains.Util;
using JetBrains.Util.Logging;
using Microsoft.Extensions.Logging;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasmAdapters;

public static class JitDisasmLoggerFactory
{
    public static Microsoft.Extensions.Logging.ILogger Create<T>()
    {
        var jetBrainsLogger = Logger.GetLogger(typeof(T));
        return new ReSharperLoggerAdapter(jetBrainsLogger);
    }
}


public class ReSharperLoggerAdapter : Microsoft.Extensions.Logging.ILogger
{
    private readonly JetBrains.Util.ILogger _logger;

    public ReSharperLoggerAdapter(JetBrains.Util.ILogger logger)
    {
        _logger = logger ?? throw new ArgumentNullException(nameof(logger));
    }

    public IDisposable BeginScope<TState>(TState state)
    {
        return NoOpDisposable.Instance;
    }

    public bool IsEnabled(LogLevel logLevel)
    {
        return true;
    }

    public void Log<TState>(LogLevel logLevel, EventId eventId, TState state, Exception exception, Func<TState, Exception, string> formatter)
    {
        if (!IsEnabled(logLevel))
        {
            return;
        }

        var message = formatter(state, exception);

        if (exception != null)
        {
            message = $"{message}\n{exception}";
        }

        switch (logLevel)
        {
            case LogLevel.Trace:
                _logger.Trace(message);
                break;

            case LogLevel.Debug:
                _logger.Verbose(message);
                break;

            case LogLevel.Information:
                _logger.Info(message);
                break;

            case LogLevel.Warning:
                _logger.Warn(message);
                break;

            case LogLevel.Error:
                _logger.Error(message);
                break;

            case LogLevel.Critical:
                _logger.Error($"[CRITICAL] {message}");
                break;

            case LogLevel.None:
                break;
        }
    }

    private class NoOpDisposable : IDisposable
    {
        public static readonly NoOpDisposable Instance = new NoOpDisposable();
        public void Dispose() { }
    }
}
