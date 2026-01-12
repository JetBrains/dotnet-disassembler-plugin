using System.Threading;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.ProjectModel;
using Microsoft.Extensions.Logging;
using ReSharperPlugin.DotNetDisassembler.JitDisasmAdapters;
using ReSharperPlugin.DotNetDisassembler.JitDisasm;

namespace ReSharperPlugin.DotNetDisassembler;

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

        if (target.IsGenericMethod && !configuration.RunAppMode)
            return Result.FailWithValue(new Error(AsmViewerErrorCode.GenericMethodsRequireRunMode));

        usageCollector.LogConfigurationSaved(configuration);
        usageCollector.LogProjectInfo(projectContext);

        var provider = new JitCodegenProvider(JitCodegenProviderLogger);
        var result = await provider.GetJitCodegenAsync(target, projectContext, configuration, cancellationToken);

        if (!result.Succeed || result.Value == null)
            return Result.FailWithValue(result.FailValue);

        return Result.Success(result.Value.Result);
    }
}
