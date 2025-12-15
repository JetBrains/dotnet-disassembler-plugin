using System;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record JitDisasmTargetFramework(string UniqueString, Version Version, bool IsNetCore)
{
    public override string ToString() => UniqueString;
}