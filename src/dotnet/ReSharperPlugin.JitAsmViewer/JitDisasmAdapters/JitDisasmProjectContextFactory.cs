using JetBrains.Application.Threading;
using JetBrains.ProjectModel;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

public static class JitDisasmProjectContextFactory
{
    public static JitDisasmProjectContext Create(IProject project)
    {
        var tfm = project.TargetFrameworkIds.TakeMax(x => x.Version.Major, min: 0);
        
        var solution = project.GetSolution();
        var outputPath = solution.Locks.ExecuteWithReadLock(() => 
            project.GetProperty(new Key("OutputPath")) as string);
        var assemblyName = solution.Locks.ExecuteWithReadLock(() =>
            project.GetProperty(new Key("AssemblyName")) as string);

        return new JitDisasmProjectContext(
            Tfm: JitDisasmTargetFrameworkFactory.Create(tfm),
            OutputPath: outputPath ?? "bin",
            ProjectFilePath: project.ProjectFileLocation.FullPath,
            ProjectDirectory: project.Location.FullPath,
            AssemblyName: assemblyName);
    }
}
