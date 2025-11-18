using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Psi.Tree;
using Microsoft.Extensions.Logging;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmCompilationService
{
    private static readonly ILogger AsmCompilationServiceLogger = JitDisasmLoggerFactory.Create<AsmCompilationService>();

    private readonly AsmViewerUsageCollector _usageCollector;

    public AsmCompilationService(AsmViewerUsageCollector usageCollector)
    {
        _usageCollector = usageCollector;
    }

    public async Task<Result<string, Error>> CompileAsync(
        IDeclaration declaration,
        JitDisasmConfiguration configuration,
        CancellationToken cancellationToken)
    {
        var currentElement = declaration.DeclaredElement;

        var validationResult = configuration.Validate();
        if (!validationResult.Succeed)
            return Result.FailWithValue(validationResult.FailValue);

        var project = declaration.GetProject();
        if (project != null)
        {
            _usageCollector.LogProjectInfo(project);
        }

        var target = JitDisasmTargetUtils.GetTarget(currentElement);
        if (target == null)
            return Result.FailWithValue(new Error(AsmViewerErrorCode.DisassemblyTargetNotFound));

        var projectContext = JitDisasmProjectContextFactory.Create(project);
        var provider = new JitCodegenProvider(AsmCompilationServiceLogger);
        var result = await provider.GetJitCodegenAsync(target, projectContext, configuration, cancellationToken);

        if (!result.Succeed || result.Value == null)
            return Result.FailWithValue(result.FailValue);

        return Result.Success(result.Value.Result);
    }
}
