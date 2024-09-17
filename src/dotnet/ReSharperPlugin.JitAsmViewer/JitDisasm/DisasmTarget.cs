using System.Collections.Generic;
using JetBrains.Annotations;

namespace ReSharperPlugin.JitAsmViewer.JitDisasm;

public record DisasmTarget(string Target, string ClassName, string MethodName, [CanBeNull] ICollection<string> MethodGenericInstantiation = null);