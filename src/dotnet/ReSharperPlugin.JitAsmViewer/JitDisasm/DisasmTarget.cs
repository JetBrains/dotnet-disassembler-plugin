using System.Collections.Generic;
using JetBrains.Annotations;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record DisasmTarget(
    string MemberFilter,
    string ClassName,
    string MethodName,
    bool IsGenericMethod = false,
    [CanBeNull] ICollection<string> MethodGenericInstantiation = null);