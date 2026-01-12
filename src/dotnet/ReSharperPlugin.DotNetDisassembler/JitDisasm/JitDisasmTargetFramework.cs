using System;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public record JitDisasmTargetFramework(string UniqueString, Version Version, bool IsNetCore)
{
    public override string ToString() => UniqueString;
}