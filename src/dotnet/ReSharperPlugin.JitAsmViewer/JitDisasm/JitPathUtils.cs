using System.IO;
using System.Linq;
using JetBrains.Core;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public class JitPathUtils
{
    public static Result<string> GetPathToRuntimePack(string pathToLocalCoreClr, string arch)
    {
        var result = GetPathToCoreClrChecked(pathToLocalCoreClr, arch);
        if (!result.Succeed)
            return Result.Fail(result.FailMessage);

        string runtimePacksPath = Path.Combine(pathToLocalCoreClr, @"artifacts\bin\runtime");
        string runtimePackPath = null;
        if (Directory.Exists(runtimePacksPath))
        {
            var packs = Directory.GetDirectories(runtimePacksPath, "*-windows-Release-" + arch);
            runtimePackPath = packs.OrderByDescending(i => i).FirstOrDefault();
        }

        if (!Directory.Exists(runtimePackPath))
        {
            var msg =
                $"""
                 Please, build a runtime-pack in your local repo:

                 Run 'build.cmd Clr+Clr.Aot+Libs -c Release -a {arch}' in the repo root
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
            var msg =
                $"""
                 Path to a local dotnet/runtime repository is either not set or it's not built for {arch} arch yet
                                 {(isCrossgenSelected
                                     ? "\n(When you use crossgen and target e.g. arm64 you need coreclr built for that arch)"
                                     : "")}
                                 \nPlease clone it and build it in `Checked` mode, e.g.:\n\n
                                 git clone git@github.com:dotnet/runtime.git\n
                                 cd runtime\n
                                 build.cmd Clr+Clr.Aot+Libs -c Release -rc Checked -a {arch}\n\n
                 """;
            return Result.Fail(msg);
        }

        return Result.Success(clrCheckedFilesDir);
    }


    public static Result<string> GetPathToCoreClrCheckedForNativeAot(string pathToLocalCoreClr, string arch)
    {
        var releaseFolder = Path.Combine(pathToLocalCoreClr, "artifacts", "bin", "coreclr", $"windows.{arch}.Checked");
        if (!Directory.Exists(releaseFolder) || !Directory.Exists(Path.Combine(releaseFolder, "aotsdk")) ||
            !Directory.Exists(Path.Combine(releaseFolder, "ilc")))
        {
            var msg =
                $"""
                 Path to a local dotnet/runtime repository is either not set or it's not correctly built for {arch} arch yet for NativeAOT
                 Please clone it and build it using the following steps.:

                 git clone git@github.com:dotnet/runtime.git
                 cd runtime
                 build.cmd Clr+Clr.Aot+Libs -c Release -rc Checked -a {arch}
                 """;
            return Result.Fail(msg);
        }

        return Result.Success(releaseFolder);
    }

    public static string FindJitDirectory(string basePath, string arch)
    {
        string jitDir = Path.Combine(basePath, $@"artifacts\bin\coreclr\windows.{arch}.Checked");
        if (Directory.Exists(jitDir))
        {
            return jitDir;
        }

        jitDir = Path.Combine(basePath, $@"artifacts\bin\coreclr\windows.{arch}.Debug");
        if (Directory.Exists(jitDir))
        {
            return jitDir;
        }

        return null;
    }
}