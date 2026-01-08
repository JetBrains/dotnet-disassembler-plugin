using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Text;
using System.Threading;
using System.Threading.Tasks;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public static class ProcessUtils
{
    public static async ValueTask<ProcessResult> RunProcessAsync(
        string path,
        string args = "",
        Dictionary<string, string> envVars = null,
        string workingDir = null,
        Action<bool, string> outputLogger = null,
        TimeSpan timeout = default,
        CancellationToken cancellationToken = default)
    {
        var logger = new StringBuilder();
        var loggerForErrors = new StringBuilder();
        var loggerLock = new object();
        Process process = null;

        using var timeoutCts = CreateTimeoutCts(timeout, cancellationToken);
        var effectiveToken = timeoutCts?.Token ?? cancellationToken;

        try
        {
            var processStartInfo = new ProcessStartInfo
            {
                FileName = path,
                UseShellExecute = false,
                CreateNoWindow = true,
                RedirectStandardError = true,
                RedirectStandardOutput = true,
                Arguments = args,
            };

            if (workingDir != null)
                processStartInfo.WorkingDirectory = workingDir;

            if (envVars != null)
            {
                foreach (var envVar in envVars)
                    processStartInfo.EnvironmentVariables[envVar.Key] = envVar.Value;
            }

            effectiveToken.ThrowIfCancellationRequested();
            process = Process.Start(processStartInfo);
            effectiveToken.ThrowIfCancellationRequested();

            process!.ErrorDataReceived += (sender, e) =>
            {
                outputLogger?.Invoke(true, e.Data + "\n");
                lock (loggerLock)
                {
                    logger.AppendLine(e.Data);
                    loggerForErrors.AppendLine(e.Data);
                }
            };
            process.OutputDataReceived += (sender, e) =>
            {
                outputLogger?.Invoke(false, e.Data + "\n");
                lock (loggerLock)
                {
                    logger.AppendLine(e.Data);
                }
            };
            process.BeginOutputReadLine();
            process.BeginErrorReadLine();

            await process.WaitForExitAsync(effectiveToken);

            return new ProcessResult { Error = loggerForErrors.ToString().Trim('\r', '\n'), Output = logger.ToString().Trim('\r', '\n') };
        }
        catch (OperationCanceledException)
        {
            return new ProcessResult
            {
                Output = logger.ToString().Trim('\r', '\n'),
                Error = loggerForErrors.ToString().Trim('\r', '\n'),
                IsTimeout = effectiveToken.IsCancellationRequested && !cancellationToken.IsCancellationRequested,
                IsCancelled = true
            };
        }
        catch (Exception e)
        {
            return new ProcessResult
            {
                Error = $"RunProcess failed:{e.Message}.\npath={path}\nargs={args}\nworkingdir={workingDir ?? Environment.CurrentDirectory}\n{loggerForErrors}",
                Exception = e
            };
        }
        finally
        {
            process.KillProccessSafe();
        }
    }

    private static CancellationTokenSource CreateTimeoutCts(TimeSpan timeout, CancellationToken cancellationToken)
    {
        if (timeout <= TimeSpan.Zero)
            return null;

        var cts = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
        cts.CancelAfter(timeout);
        return cts;
    }

    public static Task WaitForExitAsync(this Process process,
        CancellationToken cancellationToken = default(CancellationToken))
    {
        if (process.HasExited)
            return Task.CompletedTask;

        var tcs = new TaskCompletionSource<object>();
        process.EnableRaisingEvents = true;
        process.Exited += (sender, args) => tcs.TrySetResult(null);

        if (cancellationToken != default(CancellationToken))
        {
            cancellationToken.Register(() =>
            {
                process.KillProccessSafe();
                tcs.TrySetCanceled(cancellationToken);
            });
        }

        return process.HasExited ? Task.CompletedTask : tcs.Task;
    }

    private static void KillProccessSafe(this Process process)
    {
        if (process == null)
            return;
        try
        {
            if (!process.HasExited)
                process.Kill();
        }
        catch (Exception exc)
        {
            Debug.WriteLine(exc);
        }
    }

    private static string DumpEnvVars(Dictionary<string, string> envVars)
    {
        if (envVars == null)
            return "";

        string envVar = "";
        foreach (var ev in envVars)
            envVar += ev.Key + "=" + ev.Value + "\n";
        return envVar;
    }
}

public class ProcessResult
{
    public string Output { get; set; }
    public string Error { get; set; }
    public Exception Exception { get; set; }
    public bool IsTimeout { get; set; }
    public bool IsCancelled { get; set; }
    public bool IsSuccessful => Exception == null && string.IsNullOrEmpty(Error) && !IsCancelled;
}