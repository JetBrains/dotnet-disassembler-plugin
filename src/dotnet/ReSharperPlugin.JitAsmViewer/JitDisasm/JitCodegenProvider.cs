using System;
using System.Collections.Generic;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Core;
using JetBrains.ProjectModel;
using JetBrains.Util;
using JetBrains.Util.Dotnet.TargetFrameworkIds;
using JetBrains.Util.Logging;
using Key = JetBrains.Util.Key;
using Result = JetBrains.Core.Result;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public class JitCodegenProvider(IProject project)
{
    private readonly IProject _project = project;
    private ILogger _logger = Logger.GetLogger<JitCodegenProvider>();
    
    public async Task<Result<JitCodeGenResult>> GetJitCodegen(DisasmTarget target, JitDisasmConfiguration configuration,
        CancellationToken cancellationToken)
    {
        var validationResult = configuration.Validate();
        if (!validationResult.Succeed)
            return Result.Fail(validationResult.FailMessage);

        string clrCheckedFilesDir = null;
        if (configuration.UseCustomRuntime)
        {
            var result = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch,
                configuration.CrossgenIsSelected);
            if (!result.Succeed)
                return Result.Fail(result.FailMessage);
            clrCheckedFilesDir = result.Value;
        }

        cancellationToken.ThrowIfCancellationRequested();
        var tfm = string.IsNullOrWhiteSpace(configuration.OverridenTFM)
            ? project.TargetFrameworkIds.TakeMax(x => x.Version.Major, 6)
            : TargetFrameworkId.Create(configuration.OverridenTFM);

        if (tfm.IsNullOrDefault() || !tfm.IsNetCoreApp)
            return Result.Fail(
                """
                Only net6.0 (and newer) apps are supported.
                Make sure <TargetFramework>net6.0</TargetFramework> is set in your csproj.
                """);
        if (configuration.UseCustomRuntime && tfm.Version.Major < 7)
            return Result.Fail(
                """
                Only net7.0 (and newer) apps are supported with non-locally built dotnet/runtime.
                Make sure <TargetFramework>net7.0</TargetFramework> is set in your csproj.
                """);

        var projectOutputPath = project.GetProperty(new Key("OutputPath")) as string;
        var outputDir = string.IsNullOrWhiteSpace(projectOutputPath) ? "bin" : projectOutputPath;
        var resultOutDir = Path.Combine(outputDir,
            "JITDISASM" + (configuration.UseDotnetPublishForReload ? "_published" : ""));
        var projectPath = project.ProjectFileLocation.FullPath;
        var currentProjectDirPath = Path.GetDirectoryName(projectPath);
        
        if (configuration.IsNonCustomDotnetAotMode())
        {
            cancellationToken.ThrowIfCancellationRequested();
            return await GetJitCodegenInternal(target, tfm, configuration, resultOutDir, cancellationToken);
        }

        string tfmPart = configuration.DontGuessTFM && string.IsNullOrWhiteSpace(configuration.OverridenTFM)
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
            string dotnetPublishArgs =
                $"publish {tfmPart} -r win-{configuration.Arch} -c Release -o {resultOutDir} --self-contained true /p:PublishTrimmed=false /p:PublishSingleFile=false /p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\" /p:WarningLevel=0 /p:TreatWarningsAsErrors=false -v:q";

            publishResult = await ProcessUtils.RunProcess("dotnet", dotnetPublishArgs, null, currentProjectDirPath,
                cancellationToken: cancellationToken);
        }
        else
        {
            if (configuration.UseCustomRuntime)
            {
                var result = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!result.Succeed)
                    return Result.Fail(result.FailMessage);
            }
            
            string dotnetBuildArgs = $"build {tfmPart} -c Release -o {resultOutDir} --no-self-contained " +
                                     "/p:RuntimeIdentifier=\"\" " +
                                     "/p:RuntimeIdentifiers=\"\" " +
                                     "/p:WarningLevel=0 " +
                                     $"/p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\" " +
                                     $"/p:TreatWarningsAsErrors=false \"{projectPath}\"";

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

            publishResult = await ProcessUtils.RunProcess("dotnet", dotnetBuildArgs, fasterBuildEnvVars,
                currentProjectDirPath,
                cancellationToken: cancellationToken);
        }

        File.Delete(tmpProps);
        cancellationToken.ThrowIfCancellationRequested();
        
        if (!string.IsNullOrEmpty(publishResult.Error))
        {
            Result.Fail(publishResult.Error);
        }
        
        // in case if there are compilation errors:
        if (publishResult.Output.Contains(": error"))
        {
            Result.Fail(publishResult.Output);
        }
        
        if (configuration.UseDotnetPublishForReload && configuration.UseCustomRuntime)
        {
            var dstFolder = resultOutDir;
            if (!Path.IsPathRooted(dstFolder))
                dstFolder = Path.Combine(currentProjectDirPath, resultOutDir);
            if (!Directory.Exists(dstFolder))
            {
                return Result.Fail($"Something went wrong, {dstFolder} doesn't exist after 'dotnet publish -r win-{configuration.Arch} -c Release' step");
            }

            var copyClrFilesResult = await ProcessUtils.RunProcess("robocopy",
                $"/e \"{clrCheckedFilesDir}\" \"{dstFolder}", null, cancellationToken: cancellationToken);

            if (!string.IsNullOrEmpty(copyClrFilesResult.Error))
            {
                return Result.Fail(copyClrFilesResult.Error);
            }
        }
        cancellationToken.ThrowIfCancellationRequested();
        return await GetJitCodegenInternal(target, tfm, configuration, resultOutDir, cancellationToken);
    }

    private async Task<Result<JitCodeGenResult>> GetJitCodegenInternal(DisasmTarget target, TargetFrameworkId tfm,
        JitDisasmConfiguration configuration, string destFolder,
        CancellationToken cancellationToken)
    {
        try
        {
            var projectPath = project.ProjectFileLocation.FullPath;
            if (string.IsNullOrWhiteSpace(projectPath))
                return Result.Fail($"{nameof(GetJitCodegenInternal)}: {nameof(projectPath)} is null or empty");

            string fgPngPath = null;

            string dstFolder = destFolder;
            if (!Path.IsPathRooted(dstFolder))
                dstFolder = Path.Combine(Path.GetDirectoryName(projectPath), destFolder);

            string fileName = Path.GetFileNameWithoutExtension(projectPath);

            try
            {
                    var customAsmName = project.GetProperty(new Key("AssemblyName")) as string;
                    if (!string.IsNullOrWhiteSpace(customAsmName))
                    {
                        fileName = customAsmName;
                    }
            }
            catch(Exception e)
            {
                _logger.Error(e);                
            }

            var envVars = new Dictionary<string, string>();

            if (!configuration.RunAppMode && !configuration.CrossgenIsSelected && !configuration.NativeAotIsSelected)
            {
                var addinVersion = new Version();
                await LoaderAppManager.InitLoaderAndCopyTo(tfm.ToString(), dstFolder, log =>
                {
                    /*TODO: update UI*/
                }, addinVersion, cancellationToken);
            }

            if (configuration.JitDumpInsteadOfDisasm)
                envVars["DOTNET_JitDump"] = target.Target;
            else if (configuration.PrintInlinees)
                envVars["DOTNET_JitPrintInlinedMethods"] = target.Target;
            else
                envVars["DOTNET_JitDisasm"] = target.Target;

            if (!string.IsNullOrWhiteSpace(configuration.SelectedCustomJit) && !configuration.CrossgenIsSelected &&
                !configuration.NativeAotIsSelected &&
                !configuration.SelectedCustomJit.Equals(JitDisasmConfiguration.DefaultJit,
                    StringComparison.InvariantCultureIgnoreCase) && configuration.UseCustomRuntime)
            {
                envVars["DOTNET_AltJitName"] = configuration.SelectedCustomJit;
                envVars["DOTNET_AltJit"] = target.Target;
            }

            envVars["DOTNET_TieredPGO"] = configuration.UsePGO ? "1" : "0";
            envVars["DOTNET_JitDisasmDiffable"] = configuration.Diffable ? "1" : "0";

            if (!configuration.UseDotnetPublishForReload && configuration.UseCustomRuntime)
            {
                var runtimePackPath = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!runtimePackPath.Succeed)
                    return Result.Fail(runtimePackPath.FailMessage);

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
                    return Result.Fail("Flowgraph for classes (all methods) is not supported yet.");
                }

                currentFgFile = Path.GetTempFileName();
                envVars["DOTNET_JitDumpFg"] = target.Target;
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

            string executable = "dotnet";

            if (configuration.CrossgenIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrChecked = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch, configuration.CrossgenIsSelected);
                if (!pathToCoreClrChecked.Succeed)
                    return Result.Fail(pathToCoreClrChecked.FailMessage);

                var runtimePackPath = JitPathUtils.GetPathToRuntimePack(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!runtimePackPath.Succeed)
                    return Result.Fail(runtimePackPath.FailMessage);

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
                    command += $" -r: \"{dstFolder}\\*.dll\" ";
                }
                else
                {
                    // the runtime pack we use doesn't contain corelib so let's use "checked" corelib
                    // TODO: build proper core_root with release version of corelib
                    var corelib = Path.Combine(pathToCoreClrChecked.Value, "System.Private.CoreLib.dll");
                    command += $" -r: \"{runtimePackPath}\\*.dll\" -r: \"{corelib}\" ";
                }

                _logger.Verbose("Executing crossgen2...");
                _logger.Trace($"target: {target}\n{tfm}\n{configuration}");
            }
            else if (configuration.NativeAotIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrCheckedForNativeAot = JitPathUtils.GetPathToCoreClrCheckedForNativeAot(configuration.PathToLocalCoreClr, configuration.Arch);
                if (!pathToCoreClrCheckedForNativeAot.Succeed)
                    return Result.Fail(pathToCoreClrCheckedForNativeAot.FailMessage);

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
                    command += $" -r: \"{dstFolder}\\*.dll\" ";
                }
                else
                {
                    // the runtime pack we use doesn't contain corelib so let's use "checked" corelib
                    // TODO: build proper core_root with release version of corelib
                    //var corelib = Path.Combine(clrCheckedFilesDir, "System.Private.CoreLib.dll");
                    //command += $" -r: \"{runtimePackPath}\\*.dll\" -r: \"{corelib}\" ";
                }
                _logger.Verbose("Executing ILC... Make sure your method is not inlined and is reachable as NativeAOT runs IL Link. It might take some time...");
            }
            else if (configuration.IsNonCustomNativeAOTMode())
            {
                _logger.Verbose("Compiling for NativeAOT (.NET 8.0+ is required) ...");

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

                string tfmPart = configuration.DontGuessTFM && string.IsNullOrWhiteSpace(configuration.OverridenTFM)
                    ? ""
                    : $"-f {tfm}";

                // NOTE: CustomBeforeDirectoryBuildProps is probably not a good idea to overwrite, but we need to pass IlcArgs somehow
                string dotnetPublishArgs =
                    $"publish {tfmPart} -r win-{configuration.Arch} -c Release" +
                    $" /p:PublishAot=true /p:CustomBeforeDirectoryBuildProps=\"{tmpProps}\"" +
                    $" /p:WarningLevel=0 /p:TreatWarningsAsErrors=false -v:q";

                var publishResult = await ProcessUtils.RunProcess("dotnet", dotnetPublishArgs, null,
                    Path.GetDirectoryName(projectPath), cancellationToken: cancellationToken);

                cancellationToken.ThrowIfCancellationRequested();

                if (string.IsNullOrEmpty(publishResult.Error))
                {
                    if (!File.Exists(tmpJitStdout))
                    {
                        var output = $"""
                                  JitDisasm didn't produce any output :(. Make sure your method is not inlined by the code generator
                                  (it's a good idea to mark it as [MethodImpl(MethodImplOptions.NoInlining)]) and is reachable from Main() as
                                  NativeAOT may delete unused methods. Also, JitDisasm doesn't work well for Main() in NativeAOT mode."


                                  {publishResult.Output}
                                  """;
                        _logger.Verbose(output);
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
                    return Result.Fail(publishResult.Error);
                }
            }
            else
            {
                _logger.Verbose("Executing DisasmoLoader...");
            }


            if (!configuration.UseDotnetPublishForReload && !configuration.CrossgenIsSelected &&
                !configuration.NativeAotIsSelected && configuration.UseCustomRuntime)
            {
                var pathToCoreClrChecked = JitPathUtils.GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch, configuration.CrossgenIsSelected);
                if (!pathToCoreClrChecked.Succeed)
                    return Result.Fail(pathToCoreClrChecked.FailMessage);

                executable = Path.Combine(pathToCoreClrChecked.Value, "CoreRun.exe");
            }

            if ((configuration.RunAppMode) &&
                !string.IsNullOrWhiteSpace(configuration.OverridenJitDisasm))
            {
                envVars["DOTNET_JitDisasm"] = configuration.OverridenJitDisasm;
            }

            ProcessResult result = await ProcessUtils.RunProcess(
                executable, command, envVars, dstFolder, cancellationToken: cancellationToken);

            cancellationToken.ThrowIfCancellationRequested();

            if (string.IsNullOrEmpty(result.Error))
            {
                if (configuration.JitDumpInsteadOfDisasm || configuration.PrintInlinees)
                    return Result.Success(new JitCodeGenResult(result.Output));
                return Result.Success(new JitCodeGenResult(DisassemblyPrettifier.Prettify(result.Output, !configuration.ShowAsmComments && !configuration.RunAppMode)));
            }

            return Result.Fail(result.Output + "\nERROR:\n" + result.Error);

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
            }*/

            return Result.Fail("TODO");
        }
        catch (OperationCanceledException e)
        {
            return Result.Canceled(e);
        }
        catch (Exception e)
        {
            return Result.Fail(e);
        }
    }
}