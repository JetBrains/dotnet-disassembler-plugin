using System.IO;
using System.Linq;
using System.Runtime.InteropServices;
using JetBrains.Core;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public class JitPathUtils
{
    private static readonly string OsPrefix =
        RuntimeInformation.IsOSPlatform(OSPlatform.OSX) ? "osx" :
        RuntimeInformation.IsOSPlatform(OSPlatform.Linux) ? "linux" : "windows";

    private static readonly string CoreRunExecutableName =
        RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "CoreRun.exe" : "corerun";

    public static string CoreRunExecutable => CoreRunExecutableName;

    public static Result<string> GetPathToRuntimePack(string pathToLocalCoreClr, string arch)
    {
        var result = GetPathToCoreClrChecked(pathToLocalCoreClr, arch);
        if (!result.Succeed)
            return Result.Fail(result.FailMessage);

        string runtimePacksPath = Path.Combine(pathToLocalCoreClr, "artifacts", "bin", "runtime");
        string runtimePackPath = null;
        if (Directory.Exists(runtimePacksPath))
        {
            var packs = Directory.GetDirectories(runtimePacksPath, $"*-{OsPrefix}-Release-{arch}");
            runtimePackPath = packs.OrderByDescending(i => i).FirstOrDefault();
        }

        if (!Directory.Exists(runtimePackPath))
        {
            var buildCmd = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "build.cmd" : "./build.sh";
            var msg =
                $"""
                 Please, build a runtime-pack in your local repo:

                 Run '{buildCmd} Clr+Clr.Aot+Libs -c Release -a {arch}' in the repo root
                 Don't worry, you won't have to re-build it every time you change something in jit, vm or corelib.
                 """;
            return Result.Fail(msg);
        }

        return Result.Success(runtimePackPath);
    }

    public static Result<string> GetPathToCoreClrChecked(JitDisasmConfiguration configuration) =>
        GetPathToCoreClrChecked(configuration.PathToLocalCoreClr, configuration.Arch, configuration.CrossgenIsSelected);

    public static Result<string> GetPathToCoreClrChecked(string pathToLocalCoreClr, string arch,
        bool isCrossgenSelected = false)
    {
        var clrCheckedFilesDir = FindJitDirectory(pathToLocalCoreClr, arch);
        if (string.IsNullOrWhiteSpace(clrCheckedFilesDir))
        {
            var buildCmd = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "build.cmd" : "./build.sh";
            var msg =
                $"""
                 Path to a local dotnet/runtime repository is either not set or it's not built for {arch} arch yet
                                 {(isCrossgenSelected
                                     ? "\n(When you use crossgen and target e.g. arm64 you need coreclr built for that arch)"
                                     : "")}
                                 \nPlease clone it and build it in `Checked` mode, e.g.:\n\n
                                 git clone git@github.com:dotnet/runtime.git\n
                                 cd runtime\n
                                 {buildCmd} Clr+Clr.Aot+Libs -c Release -rc Checked -a {arch}\n\n
                 """;
            return Result.Fail(msg);
        }

        return Result.Success(clrCheckedFilesDir);
    }

    public static Result<string> GetPathToCoreClrCheckedForNativeAot(string pathToLocalCoreClr, string arch)
    {
        string releaseFolder = null;
        foreach (var config in new[] { "Checked", "Debug" })
        {
            var candidate = Path.Combine(pathToLocalCoreClr, "artifacts", "bin", "coreclr", $"{OsPrefix}.{arch}.{config}");
            if (Directory.Exists(candidate) && Directory.Exists(Path.Combine(candidate, "aotsdk")) &&
                Directory.Exists(Path.Combine(candidate, "ilc")))
            {
                releaseFolder = candidate;
                break;
            }
        }

        if (releaseFolder == null)
        {
            var buildCmd = RuntimeInformation.IsOSPlatform(OSPlatform.Windows) ? "build.cmd" : "./build.sh";
            var msg =
                $"""
                 Path to a local dotnet/runtime repository is either not set or it's not correctly built for {arch} arch yet for NativeAOT
                 Please clone it and build it using the following steps.:

                 git clone git@github.com:dotnet/runtime.git
                 cd runtime
                 {buildCmd} Clr+Clr.Aot+Libs -c Release -rc Checked -a {arch}
                 """;
            return Result.Fail(msg);
        }

        return Result.Success(releaseFolder);
    }

    public static string FindJitDirectory(string basePath, string arch)
    {
        string jitDir = Path.Combine(basePath, "artifacts", "bin", "coreclr", $"{OsPrefix}.{arch}.Checked");
        if (Directory.Exists(jitDir))
            return jitDir;

        jitDir = Path.Combine(basePath, "artifacts", "bin", "coreclr", $"{OsPrefix}.{arch}.Debug");
        if (Directory.Exists(jitDir))
            return jitDir;

        return null;
    }
    
    public static void CopyDirectory(string sourceDir, string destinationDir)
    {
        var dir = new DirectoryInfo(sourceDir);
        if (!dir.Exists)
            throw new DirectoryNotFoundException($"Source directory not found: {dir.FullName}");

        var dirs = dir.GetDirectories();

        Directory.CreateDirectory(destinationDir);

        foreach (var file in dir.GetFiles())
        {
            var targetFilePath = Path.Combine(destinationDir, file.Name);
            file.CopyTo(targetFilePath, overwrite: true);
        }

        foreach (var subDir in dirs)
        {
            var newDestinationDir = Path.Combine(destinationDir, subDir.Name);
            CopyDirectory(subDir.FullName, newDestinationDir);
        }
    }
}
