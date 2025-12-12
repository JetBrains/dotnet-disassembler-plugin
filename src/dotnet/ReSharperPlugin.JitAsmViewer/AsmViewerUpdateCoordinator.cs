using System;
using System.Threading.Tasks;
using JetBrains.Application.Parts;
using JetBrains.Core;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ReSharper.Feature.Services.Protocol;
using JetBrains.Rider.Model;
using JetBrains.Util;
using Microsoft.Extensions.Caching.Memory;
using ReSharperPlugin.JitAsmViewer.JitDisasm;
using ReSharperPlugin.JitAsmViewer.JitDisasmAdapters;
using Result = JetBrains.Core.Result;
using static JetBrains.Util.Logging.Logger;

namespace ReSharperPlugin.JitAsmViewer;

[SolutionComponent(Instantiation.DemandAnyThreadSafe)]
public class AsmViewerUpdateCoordinator(
    ISolution solution,
    AsmMethodLocator methodLocator,
    AsmCompilationService compilationService)
{
    private const int CacheSize = 50;
    private static readonly TimeSpan SlidingExpiration = TimeSpan.FromMinutes(30);

    private static readonly ILogger Logger = GetLogger<AsmViewerUpdateCoordinator>();

    private readonly AsmViewerModel _model = solution.GetProtocolSolution().GetAsmViewerModel();

    private readonly Lazy<MemoryCache> _cache = new(() => new MemoryCache(new MemoryCacheOptions { SizeLimit = CacheSize }));
    private readonly object _cacheLock = new();
    private int _cacheVersion;

    public async Task<Result<string, Error>> CompileAsync(JitConfiguration jitConfiguration, Lifetime lifetime)
    {
        try
        {
            var declarationResult = methodLocator.FindDeclarationAtCaret();
            if (!declarationResult.Succeed)
                return Result.FailWithValue(declarationResult.FailValue);

            if (lifetime.IsNotAlive)
                return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));

            var declarationData = declarationResult.Value;
            var configuration = JitDisasmConfigurationFactory.Create(jitConfiguration);

            var cacheKey = new CacheKey(declarationData.FilePath, declarationData.FileStamp, declarationData.Target.MemberFilter, configuration, declarationData.ProjectContext);

            int myCacheVersion;
            lock (_cacheLock)
            {
                if (_cache.Value.TryGetValue(cacheKey, out CacheEntry entry))
                {
                    Logger.Info("Cache hit for method: {0}, returning cached result (version: {1})", declarationData.Target.MemberFilter, entry.Version);
                    return entry.Result;
                }

                myCacheVersion = ++_cacheVersion;
                Logger.Info("Cache miss for method: {0}, starting compilation (version: {1})", declarationData.Target.MemberFilter, myCacheVersion);
            }

            _model.IsLoading.Value = true;
            try
            {
                var compilationResult = await compilationService.CompileAsync(
                    declarationData.Target,
                    configuration,
                    declarationData.ProjectContext,
                    lifetime);

                if (lifetime.IsNotAlive)
                {
                    Logger.Info("Lifetime is not alive, returning cancelled");
                    return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
                }

                lock (_cacheLock)
                {
                    if (_cacheVersion != myCacheVersion)
                        return compilationResult;

                    var cacheEntryOptions = new MemoryCacheEntryOptions()
                        .SetSize(1)
                        .SetSlidingExpiration(SlidingExpiration);

                    _cache.Value.Set(cacheKey, new CacheEntry(myCacheVersion, compilationResult), cacheEntryOptions);
                    Logger.Verbose("Cache updated (version: {0}), success: {1}", myCacheVersion, compilationResult.Succeed);
                }

                return compilationResult;
            }
            finally
            {
                _model.IsLoading.Value = false;
            }
        }
        catch (OperationCanceledException)
        {
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UpdateCancelled));
        }
        catch (Exception ex)
        {
            Logger.LogException(ex);
            return Result.FailWithValue(new Error(AsmViewerErrorCode.UnknownError, ex.Message));
        }
    }

    public void InvalidateCache()
    {
        lock (_cacheLock)
        {
            if (_cache.IsValueCreated)
            {
                _cache.Value.Compact(1.0);
            }
            Logger.Info("Cache invalidated");
        }
    }

    private sealed record CacheKey(string FilePath, long FileStamp, string MethodId,
        JitDisasmConfiguration Configuration, JitDisasmProjectContext ProjectContext);

    private sealed record CacheEntry(int Version, Result<string, Error> Result);
}
