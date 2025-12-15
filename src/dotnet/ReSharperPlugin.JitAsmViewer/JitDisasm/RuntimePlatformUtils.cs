using System.Runtime.InteropServices;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public static class RuntimePlatformUtils
{
    public static string GetRuntimeId(string arch)
    {
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Windows))
            return $"win-{arch}";
        if (RuntimeInformation.IsOSPlatform(OSPlatform.OSX))
            return $"osx-{arch}";
        if (RuntimeInformation.IsOSPlatform(OSPlatform.Linux))
            return $"linux-{arch}";

        return $"win-{arch}";
    }

    public static string GetCurrentArch()
    {
        return RuntimeInformation.ProcessArchitecture switch
        {
            Architecture.X64 => "x64",
            Architecture.Arm64 => "arm64",
            Architecture.X86 => "x86",
            Architecture.Arm => "arm",
            _ => "x64"
        };
    }
}
