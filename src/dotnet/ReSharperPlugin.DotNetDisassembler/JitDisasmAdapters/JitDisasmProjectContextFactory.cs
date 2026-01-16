using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;
using JetBrains.Util;
using ReSharperPlugin.DotNetDisassembler.JitDisasm;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasmAdapters;

public static class JitDisasmProjectContextFactory
{
    public static JitDisasmProjectContext Create(IProject project)
    {
        var solution = project.GetSolution();
        return solution.Locks.ExecuteWithReadLock(() =>
        {
            var sdk = project.ProjectProperties.DotNetCorePlatform?.Sdk;
            var tfmId = project.TargetFrameworkIds.TakeMax(x => x.Version.Major, min: 0);
            var outputDirectory = tfmId != null ? project.GetOutputDirectory(tfmId) : null;
            var assemblyName = tfmId != null ? project.GetOutputAssemblyName(tfmId) : null;
            var dotNetCliExePath = GetDotNetCliExePath(solution);

            return new JitDisasmProjectContext(
                Sdk: sdk,
                Tfm: tfmId != null ? JitDisasmTargetFrameworkFactory.Create(tfmId) : null,
                OutputPath: outputDirectory?.FullPath,
                ProjectFilePath: project.ProjectFileLocation?.FullPath,
                ProjectDirectory: project.Location?.FullPath,
                AssemblyName: assemblyName,
                DotNetCliExePath: dotNetCliExePath);
        });
    }

    private static string GetDotNetCliExePath(ISolution solution)
    {
        var protocolSolution = solution.GetProtocolSolution();
        var runtimeModel = protocolSolution.GetDotNetActiveRuntimeModel();
        var activeRuntime = runtimeModel.ActiveRuntime.Maybe.ValueOrDefault;
        return activeRuntime?.DotNetCliExePath?.Value ?? "dotnet";
    }
}
