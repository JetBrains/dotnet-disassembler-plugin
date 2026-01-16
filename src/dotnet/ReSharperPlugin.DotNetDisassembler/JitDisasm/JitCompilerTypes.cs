using System.Collections.Generic;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public static class JitCompilerTypes
{
    public const string DefaultJit = "clrjit.dll";
    public const string Crossgen = "crossgen2.dll";
    public const string Ilc = "ilc";

    public static IReadOnlyList<string> All { get; } =
    [
        DefaultJit,
        Crossgen,
        Ilc
    ];
}
