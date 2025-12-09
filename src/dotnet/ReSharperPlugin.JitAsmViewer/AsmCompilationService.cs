using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.ProjectModel;
using Microsoft.Extensions.Logging;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmCompilationService(AsmViewerUsageCollector usageCollector)
{
    private static readonly ILogger JitCodegenProviderLogger = JitDisasmLoggerFactory.Create<JitCodegenProvider>();

    public async Task<Result<string, Error>> CompileAsync(
        DisasmTarget target,
        JitDisasmConfiguration configuration,
        JitDisasmProjectContext projectContext,
        CancellationToken cancellationToken)
    {
        var validationResult = configuration.Validate();
        if (!validationResult.Succeed)
            return Result.FailWithValue(validationResult.FailValue);

        usageCollector.LogConfigurationSaved(configuration);
        usageCollector.LogProjectInfo(projectContext);

        var provider = new JitCodegenProvider(JitCodegenProviderLogger);
        var result = await provider.GetJitCodegenAsync(target, projectContext, configuration, cancellationToken);

        if (!result.Succeed || result.Value == null)
            return Result.FailWithValue(result.FailValue);

        return Result.Success(result.Value.Result);
    }
}
