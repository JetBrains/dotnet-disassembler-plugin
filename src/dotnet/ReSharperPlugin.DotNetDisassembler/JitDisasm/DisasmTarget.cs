using System.Collections.Generic;
using JetBrains.Annotations;

namespace ReSharperPlugin.DotNetDisassembler.JitDisasm;

public record DisasmTarget(
    string MemberFilter,
    string ClassName,
    string MethodName,
    bool IsGenericMethod = false,
    [CanBeNull] ICollection<string> MethodGenericInstantiation = null);