using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.Rd.Tasks;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;
using JetBrains.Util;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.ContainerAsyncPrimaryThread)]
public class AsmViewerHost
{
    private readonly AsmViewerUpdateCoordinator _updateCoordinator;
    private readonly AsmViewerUsageCollector _usageCollector;
    private readonly ILogger _logger = GetLogger(typeof(AsmViewerHost));

    public AsmViewerHost(
        ISolution solution,
        AsmViewerUpdateCoordinator updateCoordinator,
        AsmViewerUsageCollector usageCollector)
    {
        _logger.Info("Initializing AsmViewerHost for solution: {0}", solution.Name);
        _updateCoordinator = updateCoordinator;
        _usageCollector = usageCollector;

        var model = solution.GetProtocolSolution().GetAsmViewerModel();
        model.Compile.SetAsync(OnRequestCompileAsync);
        _logger.Info("AsmViewerHost initialized successfully");
    }

    private async Task<CompilationResponse> OnRequestCompileAsync(Lifetime lifetime, CompileRequest request)
    {
        var caretPosition = request.CaretPosition;
        _logger.Info("Compile request for file: {0}, offset: {1}", caretPosition.FilePath, caretPosition.Offset);

        var result = await _updateCoordinator.CompileAsync(request, lifetime);

        if (!result.Succeed)
        {
            var error = result.FailValue;
            _logger.Warn("Disassembly failed - Code: {0}, Details: {1}", error.Code, error.Details);
            _usageCollector.LogError(error.Code);
            return new CompilationResponse(null, new ErrorInfo(error.Code.ToRdErrorCode(), error.Details));
        }

        _logger.Info("Disassembly succeeded, content length: {0}", result.Value.Length);
        _usageCollector.LogDisassemblySucceeded();
        return new CompilationResponse(result.Value, null);
    }
}
