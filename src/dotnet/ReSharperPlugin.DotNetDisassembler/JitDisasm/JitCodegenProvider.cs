using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Core;
using Microsoft.Extensions.Logging;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public class JitCodegenProvider(ILogger logger)
{
    public async Task<Result<JitCodeGenResult, Error>> GetJitCodegenAsync(DisasmTarget target, JitDisasmProjectContext projectContext,
        JitDisasmConfiguration configuration, CancellationToken cancellationToken)
    {
        logger.LogInformation("GetJitCodegen called - Target: {0}.{1}, Arch: {2}", target.ClassName, target.MethodName, configuration.Arch);

        var configValidationResult = configuration.Validate();
        if (!configValidationResult.Succeed)
            return Result.FailWithValue(configValidationResult.FailValue);

        var contextValidationResult = projectContext.Validate();
        if (!contextValidationResult.Succeed)
            return Result.FailWithValue(contextValidationResult.FailValue);

        var runtimeId = RuntimePlatformUtils.GetRuntimeId(configuration.Arch);

        cancellationToken.ThrowIfCancellationRequested();
        var tfm = configuration.OverridenTfm ?? projectContext.Tfm;
        logger.LogDebug("Target framework: {0}", tfm);

        if (!tfm.IsNetCore)
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnsupportedTargetFramework));

        if (configuration.UseCustomRuntime && tfm.Version.Major < 7)
            return Result.FailWithValue(new Error(AsmViewerErrorCode.CustomRuntimeRequiresNet7));

        string clrCheckedFilesDir = null;
        if (configuration.UseCustomRuntime)
        {
            logger.LogDebug("Using custom runtime from: {0}", configuration.PathToLocalCoreClr);

            var result = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch,
                configuration.CrossgenIsSelected);
            if (!result.Succeed)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.CoreClrCheckedNotFound, result.FailMessage));
            clrCheckedFilesDir = result.Value;
        }

        var resultOutDir = configuration.UseDotnetPublishForReload
            ? Path.Combine(projectContext.OutputPath!, "DotNetDisassembler_published", tfm.UniqueString, runtimeId)
            : Path.Combine(projectContext.OutputPath!, "DotNetDisassembler", tfm.UniqueString);

        var dotnetCliExePath = projectContext.DotNetCliExePath!;
        var projectFilePath = projectContext.ProjectFilePath!;
        var projectDirPath = projectContext.ProjectDirectory!;

        if (configuration.IsNonCustomDotnetAotMode())
        {
            cancellationToken.ThrowIfCancellationRequested();
            return await GetJitCodegenInternalAsync(target, tfm, configuration, projectContext, resultOutDir, dotnetCliExePath, runtimeId, cancellationToken);
        }

        string tfmPart = configuration.DontGuessTfm && configuration.OverridenTfm == null
            ? ""
            : $"-f {tfm}";

        // Some things can't be set in CLI e.g. appending to DefineConstants
        var tmpProps = Path.GetTempFileName() + ".props";
        File.WriteAllText(tmpProps, $"""
                                     <?xml version="1.0" encoding="utf-8"?>
                                     <Project>
                                         <PropertyGroup>
                                             <DefineConstants>$(DefineConstants);DISASMO</DefineConstants>
                                         </PropertyGroup>
                                     </Project>
                                     """);
        ProcessResult publishResult;
        if (configuration.UseDotnetPublishForReload)
        {
            logger.LogInformation("Running dotnet publish for reload");

            string dotnetPublishArgs =
                $"publish {tfmPart} -r {runtimeId} -c Release -o \"{resultOutDir}\" --self-contained true /p:PublishTrimmed=false /p:PublishSingleFile=false /p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\" /p:WarningLevel=0 /p:TreatWarningsAsErrors=false -v:q";

            publishResult = await ProcessUtils.RunProcessAsync(dotnetCliExePath, dotnetPublishArgs, null, projectDirPath,
                LogProcessOutput, cancellationToken: cancellationToken);
        }
        else
        {
            logger.LogInformation("Running dotnet build");

            if (configuration.UseCustomRuntime)
            {
                var result = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!result.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.RuntimePackNotFound, result.FailMessage));
            }

            string dotnetBuildArgs = $"build {tfmPart} -c Release -o \"{resultOutDir}\" --no-self-contained " +
                                     "/p:RuntimeIdentifier=\"\" " +
                                     "/p:RuntimeIdentifiers=\"\" " +
                                     "/p:WarningLevel=0 " +
                                     $"/p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\" " +
                                     $"/p:TreatWarningsAsErrors=false \"{projectFilePath}\"";

            Dictionary<string, string> fasterBuildEnvVars = new Dictionary<string, string>
            {
                ["DOTNET_SKIP_FIRST_TIME_EXPERIENCE"] = "1",
                ["DOTNET_CLI_TELEMETRY_OPTOUT"] = "1"
            };

            if (configuration.UseNoRestoreFlag)
            {
                dotnetBuildArgs += " --no-restore --no-dependencies --nologo";
                fasterBuildEnvVars["DOTNET_MULTILEVEL_LOOKUP"] = "0";
            }

            publishResult = await ProcessUtils.RunProcessAsync(dotnetCliExePath, dotnetBuildArgs, fasterBuildEnvVars,
                projectDirPath, LogProcessOutput, cancellationToken: cancellationToken);
        }

        File.Delete(tmpProps);
        cancellationToken.ThrowIfCancellationRequested();

        if (!publishResult.IsSuccessful)
        {
            if (publishResult.Exception != null)
                logger.LogError(publishResult.Exception, "Process execution failed");

            var errorCode = configuration.UseDotnetPublishForReload
                ? AsmViewerErrorCode.DotnetPublishFailed
                : AsmViewerErrorCode.DotnetBuildFailed;
            return Result.FailWithValue(new Error(errorCode, publishResult.Error));
        }

        // in case if there are compilation errors:
        if (publishResult.Output.Contains(": error"))
        {
            var errorCode = configuration.UseDotnetPublishForReload
                ? AsmViewerErrorCode.DotnetPublishFailed
                : AsmViewerErrorCode.DotnetBuildFailed;
            return Result.FailWithValue(new Error(errorCode, publishResult.Output));
        }
        
        logger.LogInformation("Build/publish completed successfully");
        
        if (configuration.UseDotnetPublishForReload && configuration.UseCustomRuntime)
        {
            var dstFolder = resultOutDir;
            if (!Path.IsPathRooted(dstFolder))
                dstFolder = Path.Combine(projectDirPath, resultOutDir);
            if (!Directory.Exists(dstFolder))
            {
                return Result.FailWithValue(new Error(AsmViewerErrorCode.DotnetPublishFailed,
                    $"{dstFolder} doesn't exist after 'dotnet publish -r {runtimeId} -c Release' step"));
            }

            try
            {
                JitPathUtils.CopyDirectory(clrCheckedFilesDir, dstFolder);
            }
            catch (Exception ex)
            {
                logger.LogError(ex, "Failed to copy CLR checked files");
                return Result.FailWithValue(new Error(AsmViewerErrorCode.CompilationFailed, ex.Message));
            }
        }
        cancellationToken.ThrowIfCancellationRequested();
        return await GetJitCodegenInternalAsync(target, tfm, configuration, projectContext, resultOutDir, dotnetCliExePath, runtimeId, cancellationToken);
    }

    private async Task<Result<JitCodeGenResult, Error>> GetJitCodegenInternalAsync(DisasmTarget target, JitDisasmTargetFramework tfm,
        JitDisasmConfiguration configuration, JitDisasmProjectContext projectContext, string destFolder, string dotnetCliExePath,
        string runtimeId, CancellationToken cancellationToken)
    {
        try
        {
            var projectPath = projectContext.ProjectFilePath;
            if (string.IsNullOrWhiteSpace(projectPath))
                return Result.FailWithValue(new Error(AsmViewerErrorCode.ProjectPathNotFound));

            string dstFolder = destFolder;
            if (!Path.IsPathRooted(dstFolder))
                dstFolder = Path.Combine(Path.GetDirectoryName(projectPath), destFolder);

            string fileName = Path.GetFileNameWithoutExtension(projectPath);

            if (!string.IsNullOrWhiteSpace(projectContext.AssemblyName))
            {
                fileName = projectContext.AssemblyName;
            }

            var envVars = new Dictionary<string, string>();

            if (!configuration.RunAppMode && !configuration.CrossgenIsSelected && !configuration.NativeAotIsSelected)
            {
                var addinVersion = new Version();
                await LoaderAppManager.InitLoaderAndCopyToAsync(dotnetCliExePath, tfm.UniqueString, dstFolder, _ => { }, addinVersion, cancellationToken);
            }

            if (configuration.JitDumpInsteadOfDisasm)
                envVars["DOTNET_JitDump"] = target.MemberFilter;
            else
            {
                envVars["DOTNET_JitDisasm"] = target.MemberFilter;
                envVars["DOTNET_JitPrintInlinedMethods"] = target.MemberFilter;
            }

            if (!string.IsNullOrWhiteSpace(configuration.SelectedCustomJit) && !configuration.CrossgenIsSelected &&
                !configuration.NativeAotIsSelected &&
                !configuration.SelectedCustomJit.Equals(JitCompilerTypes.DefaultJit,
                    StringComparison.InvariantCultureIgnoreCase) && configuration.UseCustomRuntime)
            {
                envVars["DOTNET_AltJitName"] = configuration.SelectedCustomJit;
                envVars["DOTNET_AltJit"] = target.MemberFilter;
            }

            envVars["DOTNET_TieredPGO"] = configuration.UsePgo ? "1" : "0";
            envVars["DOTNET_JitDisasmDiffable"] = configuration.Diffable ? "1" : "0";

            if (!configuration.UseDotnetPublishForReload && configuration.UseCustomRuntime)
            {
                var runtimePackPath = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!runtimePackPath.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.RuntimePackNotFound, runtimePackPath.FailMessage));

                // tell jit to look for BCL libs in the locally built runtime pack
                envVars["CORE_LIBRARIES"] = runtimePackPath.Value;
            }

            envVars["DOTNET_TieredCompilation"] = configuration.UseTieredJit ? "1" : "0";

            // User is free to override any of those ^
            configuration.FillWithUserVars(envVars);


            string currentFgFile = null;
            if (configuration.FgEnable)
            {
                if (target.MethodName == "*")
                {
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.FlowgraphsForClassNotSupported));
                }

                currentFgFile = Path.GetTempFileName();
                envVars["DOTNET_JitDumpFg"] = target.MemberFilter;
                envVars["DOTNET_JitDumpFgDot"] = "1";
                envVars["DOTNET_JitDumpFgPhase"] = "*";
                envVars["DOTNET_JitDumpFgFile"] = currentFgFile;
            }

            string command =
                $"\"{LoaderAppManager.JitDisasmLoaderName}.dll\" \"{fileName}.dll\" \"{target.ClassName}\" \"{target.MethodName}\" {configuration.UseUnloadableContext}";
            if (configuration.RunAppMode)
            {
                command = $"\"{fileName}.dll\"";
            }

            string executable = dotnetCliExePath;

            if (configuration.CrossgenIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrChecked = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch, configuration.CrossgenIsSelected);
                if (!pathToCoreClrChecked.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.CoreClrCheckedNotFound, pathToCoreClrChecked.FailMessage));

                var runtimePackPath = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!runtimePackPath.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.RuntimePackNotFound, runtimePackPath.FailMessage));

                executable = Path.Combine(configuration.PathToLocalCoreClr, "dotnet.cmd");
                command = $"{Path.Combine(pathToCoreClrChecked.Value, "crossgen2", "crossgen2.dll")} --out aot ";

                foreach (var envVar in envVars)
                {
                    var keyLower = envVar.Key.ToLowerInvariant();
                    if (keyLower?.StartsWith("dotnet_") == false &&
                        keyLower?.StartsWith("complus_") == false)
                    {
                        continue;
                    }

                    keyLower = keyLower
                        .Replace("dotnet_jitdump", "--codegenopt:jitdump")
                        .Replace("dotnet_jitdisasm", "--codegenopt:jitdisasm")
                        .Replace("dotnet_", "--codegenopt:")
                        .Replace("complus_", "--codegenopt:");
                    command += keyLower + "=\"" + envVar.Value + "\" ";
                }

                envVars.Clear();

                // These are needed for faster crossgen itself - they're not changing output codegen
                envVars["DOTNET_TieredPGO"] = "0";
                envVars["DOTNET_ReadyToRun"] = "1";
                envVars["DOTNET_TC_QuickJitForLoops"] = "1";
                envVars["DOTNET_TC_CallCountingDelayMs"] = "0";
                envVars["DOTNET_TieredCompilation"] = "1";
                command += configuration.Crossgen2Args.Replace("\r\n", " ").Replace("\n", " ") + $" \"{fileName}.dll\" ";

                if (configuration.UseDotnetPublishForReload)
                {
                    // Reference everything in the publish dir
                    command += $" -r: \"{Path.Combine(dstFolder, "*.dll")}\" ";
                }
                else
                {
                    // the runtime pack we use doesn't contain corelib so let's use "checked" corelib
                    // TODO: build proper core_root with release version of corelib
                    var corelib = Path.Combine(pathToCoreClrChecked.Value, "System.Private.CoreLib.dll");
                    command += $" -r: \"{Path.Combine(runtimePackPath.Value, "*.dll")}\" -r: \"{corelib}\" ";
                }
                
                logger.LogDebug("Executing crossgen2...");
                logger.LogTrace($"target: {target}\n{tfm}\n{configuration}");
            }
            else if (configuration.NativeAotIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrCheckedForNativeAot = JitPathUtils.GetPathToCoreClrCheckedForNativeAot(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!pathToCoreClrCheckedForNativeAot.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.CoreClrCheckedNotFound, pathToCoreClrCheckedForNativeAot.FailMessage));

                command = "";
                executable = Path.Combine(pathToCoreClrCheckedForNativeAot.Value, "ilc", "ilc.exe");

                command += $" \"{fileName}.dll\" ";

                foreach (var envVar in envVars)
                {
                    var keyLower = envVar.Key.ToLowerInvariant();
                    if (keyLower?.StartsWith("dotnet_") == false &&
                        keyLower?.StartsWith("complus_") == false)
                    {
                        continue;
                    }

                    keyLower = keyLower
                        .Replace("dotnet_jitdump", "--codegenopt:jitdump")
                        .Replace("dotnet_jitdisasm", "--codegenopt:jitdisasm")
                        .Replace("dotnet_", "--codegenopt:")
                        .Replace("complus_", "--codegenopt:");
                    command += keyLower + "=\"" + envVar.Value + "\" ";
                }

                envVars.Clear();
                command += configuration.IlcArgs.Replace("%DOTNET_REPO%", configuration.PathToLocalCoreClr.TrimEnd('\\', '/'))
                    .Replace("\r\n", " ").Replace("\n", " ");

                if (configuration.UseDotnetPublishForReload)
                {
                    // Reference everything in the publish dir
                    command += $" -r: \"{Path.Combine(dstFolder, "*.dll")}\" ";
                }
                else
                {
                    // the runtime pack we use doesn't contain corelib so let's use "checked" corelib
                    // TODO: build proper core_root with release version of corelib
                    //var corelib = Path.Combine(clrCheckedFilesDir, "System.Private.CoreLib.dll");
                    //command += $" -r: \"{runtimePackPath}\\*.dll\" -r: \"{corelib}\" ";
                }
                logger.LogDebug("Executing ILC... Make sure your method is not inlined and is reachable as NativeAOT runs IL Link. It might take some time...");
            }
            else if (configuration.IsNonCustomNativeAotMode())
            {
                logger.LogDebug("Compiling for NativeAOT (.NET 8.0+ is required) ...");
                
                // For non-custom NativeAOT we need to use dotnet publish + with custom IlcArgs
                // namely, we need to re-direct jit's output to a file (JitStdOutFile).

                var tmpJitStdout = Path.GetTempFileName() + ".asm";

                envVars["DOTNET_JitStdOutFile"] = tmpJitStdout;

                string customIlcArgs = "";
                foreach (var envVar in envVars)
                {
                    var keyLower = envVar.Key.ToLowerInvariant();
                    if (keyLower?.StartsWith("dotnet_") == false &&
                        keyLower?.StartsWith("complus_") == false)
                    {
                        continue;
                    }

                    keyLower = keyLower
                        .Replace("dotnet_", "--codegenopt:")
                        .Replace("complus_", "--codegenopt:");
                    customIlcArgs += $"\t\t<IlcArg Include=\"{keyLower}=&quot;{envVar.Value}&quot;\" />\n";
                }

                envVars.Clear();

                var tmpProps = Path.GetTempFileName() + ".props";
                File.WriteAllText(tmpProps, $"""
                                             <?xml version="1.0" encoding="utf-8"?>
                                             <Project>
                                                 <PropertyGroup>
                                                     <DefineConstants>$(DefineConstants);DISASMO</DefineConstants>
                                                 </PropertyGroup>
                                             	<ItemGroup>
                                             {customIlcArgs}
                                             	</ItemGroup>
                                             </Project>
                                             """);

                string tfmPart = configuration.DontGuessTfm && configuration.OverridenTfm == null
                    ? ""
                    : $"-f {tfm}";

                // NOTE: CustomBeforeDirectoryBuildProps is probably not a good idea to overwrite, but we need to pass IlcArgs somehow
                string dotnetPublishArgs =
                    $"publish {tfmPart} -r {runtimeId} -c Release" +
                    $" /p:PublishAot=true /p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\"" +
                    $" /p:WarningLevel=0 /p:TreatWarningsAsErrors=false -v:q";

                var publishResult = await ProcessUtils.RunProcessAsync(dotnetCliExePath, dotnetPublishArgs, null,
                    projectContext.ProjectDirectory, LogProcessOutput, cancellationToken: cancellationToken);

                cancellationToken.ThrowIfCancellationRequested();

                if (publishResult.IsSuccessful)
                {
                    if (!File.Exists(tmpJitStdout))
                    {
                        var output = $"""
                                  JitDisasm didn't produce any output :(. Make sure your method is not inlined by the code generator
                                  (it's a good idea to mark it as [MethodImpl(MethodImplOptions.NoInlining)]) and is reachable from Main() as
                                  NativeAOT may delete unused methods. Also, JitDisasm doesn't work well for Main() in NativeAOT mode."


                                  {publishResult.Output}
                                  """;
                        logger.LogDebug(output);
                    }
                    else
                    {
                        var jitAsm = File.ReadAllText(tmpJitStdout);

                        // Keep the temp files around for debugging if it failed.
                        // and delete them if it succeeded.
                        File.Delete(tmpProps);
                        File.Delete(tmpJitStdout);
                        return Result.Success(new JitCodeGenResult(jitAsm));
                    }
                }
                else
                {
                    if (publishResult.Exception != null)
                        logger.LogError(publishResult.Exception, "Process execution failed");
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.DotnetPublishFailed, publishResult.Error));
                }
            }
            else
            {
                logger.LogDebug("Executing DisasmoLoader...");
            }


            if (!configuration.UseDotnetPublishForReload && !configuration.CrossgenIsSelected &&
                !configuration.NativeAotIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrChecked = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch, configuration.CrossgenIsSelected);
                if (!pathToCoreClrChecked.Succeed)
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.CoreClrCheckedNotFound, pathToCoreClrChecked.FailMessage));

                executable = Path.Combine(pathToCoreClrChecked.Value, "CoreRun.exe");
            }

            if ((configuration.RunAppMode) &&
                !string.IsNullOrWhiteSpace(configuration.OverridenJitDisasm))
            {
                envVars["DOTNET_JitDisasm"] = configuration.OverridenJitDisasm;
            }

            if (!Directory.Exists(dstFolder))
                Directory.CreateDirectory(dstFolder);

            logger.LogInformation("Executing process: {0} with args: {1}", executable, command.Length > 100 ? command.Substring(0, 100) + "..." : command);

            ProcessResult result = await ProcessUtils.RunProcessAsync(
                executable, command, envVars, dstFolder, LogProcessOutput,
                configuration.DisassemblyTimeout, cancellationToken);

            if (result.IsTimeout)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.DisassemblyTimeout));

            if (result.IsCancelled)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));

            if (result.IsSuccessful)
            {
                logger.LogInformation("Process execution succeeded, output length: {0}", result.Output.Length);

                if (configuration.JitDumpInsteadOfDisasm)
                    return Result.Success(new JitCodeGenResult(result.Output));
                return Result.Success(new JitCodeGenResult(DisassemblyPrettifier.Prettify(result.Output, !configuration.ShowAsmComments && !configuration.RunAppMode)));
            }

            if (result.Exception != null)
                logger.LogError(result.Exception, "Process execution failed");

            return Result.FailWithValue(new Error(AsmViewerErrorCode.CompilationFailed, result.Output + "\nERROR:\n" + result.Error));

            /*if (configuration.FgEnable && configuration.JitDumpInsteadOfDisasm)
            {
                currentFgFile += ".dot";
                if (!File.Exists(currentFgFile))
                {
                    Output = $"Oops, JitDumpFgFile ('{currentFgFile}') doesn't exist :(\nInvalid Phase name?";
                    return;
                }

                if (new FileInfo(currentFgFile).Length == 0)
                {
                    Output = $"Oops, JitDumpFgFile ('{currentFgFile}') file is empty :(\nInvalid Phase name?";
                    return;
                }

                string fgLines = File.ReadAllText(currentFgFile);

                FgPhases.Clear();
                var graphs = fgLines.Split(new[] { "digraph FlowGraph {" }, StringSplitOptions.RemoveEmptyEntries);
                int graphIndex = 0;

                var fgBaseDir = Path.Combine(Path.GetTempPath(), "Disasmo", "Flowgraphs", Guid.NewGuid().ToString("N"));
                Directory.CreateDirectory(fgBaseDir);
                foreach (var graph in graphs)
                {
                    try
                    {
                        var name = graph.Substring(graph.IndexOf("graph [label = ") + "graph [label = ".Length);
                        name = name.Substring(0, name.IndexOf("\"];"));
                        name = name.Replace("\\n", " ");
                        name = name.Substring(name.IndexOf(" after ") + " after ".Length).Trim();

                        // Reset counter if tier0 and tier1 are merged together
                        if (name == "Pre-import")
                        {
                            graphIndex = 0;
                        }

                        name = (++graphIndex) + ". " + name;

                        // Ignore invalid path chars
                        name = Path.GetInvalidFileNameChars()
                            .Aggregate(name, (current, ic) => current.Replace(ic, '_'));

                        var dotPath = Path.Combine(fgBaseDir, $"{name}.dot");
                        File.WriteAllText(dotPath, "digraph FlowGraph {\n" + graph);

                        FgPhases.Add(new FlowgraphItemViewModel(configuration)
                            { Name = name, DotFileUrl = dotPath, ImageUrl = "" });
                    }
                    catch (Exception exc)
                    {
                        Debug.WriteLine(exc);
                    }
                }
            }

            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError, "Reached unexpected code path"));*/
        }
        catch (OperationCanceledException)
        {
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
        }
        catch (Exception e)
        {
            logger.LogError(e, "Unexpected error during JIT code generation");
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError, e.Message));
        }
    }

    private void LogProcessOutput(bool isError, string message)
    {
        if (isError)
            logger.LogWarning(message);
        else
            logger.LogDebug(message);
    }
}