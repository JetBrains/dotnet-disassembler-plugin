using JetBrains.Annotations;
using JetBrains.Util.Dotnet.TargetFrameworkIds;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;

public static class JitDisasmTargetFrameworkFactory
{
    public static JitDisasmTargetFramework Create(TargetFrameworkId tfmId)
    {
        // .NET Core/5+: netcoreapp*, net5-* (exclude net1-4 = old .NET Framework, netstandard)
        var isNetCore = tfmId.UniqueString.StartsWith("netcoreapp")
                        || (tfmId.UniqueString.StartsWith("net") && tfmId.Version.Major >= 5);

        return new JitDisasmTargetFramework(tfmId.UniqueString, tfmId.Version, isNetCore);
    }

    public static JitDisasmTargetFramework Create([NotNull] string tfmString)
    {
        var tfmId = TargetFrameworkId.Create(tfmString);
        return Create(tfmId);
    }
}