using System;
using System.IO;
using System.Threading;
using System.Threading.Tasks;
using JetBrains.Annotations;
using Microsoft.Extensions.Logging;
using Moq;
using NUnit.Framework;
using ReSharperPlugin.DotNetDisassembler.JitDisasm;

namespace ReSharperPlugin.DotNetDisassembler.Tests;

[TestFixture]
public class JitCodegenProviderTests
{
    private string _testProjectDir;
    private string _testProjectFile;
    private JitCodegenProvider _jitCodegenProvider;
    private DisasmTarget _mainMethodTarget;
    private DisasmTarget _addMethodTarget;
    private DisasmTarget _valuePropertyTarget;
    private DisasmTarget _missingMethodTarget;

    [SetUp]
    public void SetUp()
    {
        _testProjectDir = Path.Combine(Path.GetTempPath(), "JitAsmViewerTests_" + Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(_testProjectDir);
        _testProjectFile = Path.Combine(_testProjectDir, "TestProject.csproj");

        File.WriteAllText(_testProjectFile, @"
            <Project Sdk=""Microsoft.NET.Sdk"">
              <PropertyGroup>
                <OutputType>Exe</OutputType>
                <TargetFramework>net8.0</TargetFramework>
                <UseAppHost>false</UseAppHost>
              </PropertyGroup>
            </Project>");

        var programFile = Path.Combine(_testProjectDir, "Program.cs");
        File.WriteAllText(programFile, @"
            using System;

            public class TestClass
            {
                private static int _value = 100;

                public static void Main()
                {
                    Console.WriteLine(Add(2, 3));
                    Console.WriteLine(Value);
                }

                public static int Add(int a, int b)
                {
                    return a + b;
                }

                public static int Value
                {
                    get { return _value; }
                }
            }");
        
        _jitCodegenProvider = new JitCodegenProvider(Mock.Of<ILogger>());
        
        _mainMethodTarget = new DisasmTarget("TestClass:Main", "TestClass", "Main");
        _addMethodTarget = new DisasmTarget("TestClass:Add", "TestClass", "Add");
        _valuePropertyTarget = new DisasmTarget("TestClass:get_Value", "TestClass", "get_Value");
        _missingMethodTarget = new DisasmTarget("TestClass:MissingMethod", "TestClass", "MissingMethod");
    }

    [TearDown]
    public void TearDown()
    {
        if (!Directory.Exists(_testProjectDir))
        {
            return;
        }
        
        try
        {
            Directory.Delete(_testProjectDir, true);
        }
        catch
        {
            // Ignore cleanup errors
        }
    }

    [Test]
    public async Task GetJitCodegen_WhenConfigurationInvalid_ShouldReturnError()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration
        {
            SelectedCustomJit = JitCompilerTypes.Crossgen,
            UsePgo = true
        };

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.NotNull(result.FailValue);
        Assert.AreEqual(AsmViewerErrorCode.PgoNotSupportedForAot, result.FailValue.Code);
    }

    [Test]
    public async Task GetJitCodegen_WhenUnsupportedTargetFramework_ShouldReturnError()
    {
        // Arrange
        var projectContext = CreateProjectContext() with
        {
            Tfm = new JitDisasmTargetFramework(
                UniqueString: "net472",
                new Version(4, 7, 2),
                IsNetCore: false)
        };

        var config = new JitDisasmConfiguration();

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.AreEqual(AsmViewerErrorCode.UnsupportedTargetFramework, result.FailValue.Code);
    }

    [Test]
    public async Task GetJitCodegen_WhenCustomRuntimeWithOldFramework_ShouldReturnError()
    {
        // Arrange
        var projectContext = CreateProjectContext() with
        {
            Tfm = new JitDisasmTargetFramework(
                UniqueString: "net6.0",
                new Version(6, 0, 0, 0),
                IsNetCore: true)
        };

        var config = new JitDisasmConfiguration
        {
            UseCustomRuntime = true,
            PathToLocalCoreClr = "/some/path"
        };

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.AreEqual(AsmViewerErrorCode.CustomRuntimeRequiresNet7, result.FailValue.Code);
    }

    [Test]
    [TestCase(JitCompilerTypes.Crossgen, true, AsmViewerErrorCode.RunModeNotSupportedForAot)]
    [TestCase(JitCompilerTypes.Ilc, true, AsmViewerErrorCode.RunModeNotSupportedForAot)]
    public async Task GetJitCodegen_WithRunAppMode_ShouldReturnError(
        string customJit, bool runAppMode, AsmViewerErrorCode expectedError)
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration
        {
            SelectedCustomJit = customJit,
            RunAppMode = runAppMode
        };

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.AreEqual(expectedError, result.FailValue.Code);
    }

    [Test]
    [TestCase(JitCompilerTypes.Crossgen, true, AsmViewerErrorCode.TieredJitNotSupportedForAot)]
    [TestCase(JitCompilerTypes.Ilc, true, AsmViewerErrorCode.TieredJitNotSupportedForAot)]
    public async Task GetJitCodegen_WithTieredJit_ShouldReturnError(
        string customJit, bool useTieredJit, AsmViewerErrorCode expectedError)
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration
        {
            SelectedCustomJit = customJit,
            UseTieredJit = useTieredJit
        };

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.AreEqual(expectedError, result.FailValue.Code);
    }

    [Test]
    public async Task GetJitCodegen_WithFlowgraphsAndAot_ShouldReturnError()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration
        {
            SelectedCustomJit = JitCompilerTypes.Crossgen,
            FgEnable = true
        };

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.False(result.Succeed);
        Assert.AreEqual(AsmViewerErrorCode.FlowgraphsNotSupportedForAot, result.FailValue.Code);
    }
    
    [Test]
    public async Task GetJitCodegen_WithMainMethod_ShouldReturnAssemblyCode()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration();

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_mainMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.True(result.Succeed, $"Error: {result.FailValue?.Code}\nDetails: {result.FailValue?.Details}");
        Assert.NotNull(result.Value);
        Assert.NotNull(result.Value.Result);
        Assert.IsNotEmpty(result.Value.Result);
        Assert.True(result.Value.Result.Contains(_mainMethodTarget.ClassName)
                    || result.Value.Result.Contains(_mainMethodTarget.MethodName),
            "Output should contain method or class name");
    }

    [Test]
    public async Task GetJitCodegen_WithMethod_ShouldReturnAssemblyCode()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration();

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_addMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.True(result.Succeed, $"Error: {result.FailValue?.Code}\nDetails: {result.FailValue?.Details}");
        Assert.NotNull(result.Value);
        Assert.NotNull(result.Value.Result);
        Assert.IsNotEmpty(result.Value.Result);
        Assert.True(result.Value.Result.Contains(_addMethodTarget.ClassName)
                    || result.Value.Result.Contains(_addMethodTarget.MethodName),
            "Output should contain method or class name");
    }

    [Test]
    public async Task GetJitCodegen_WithProperty_ShouldReturnAssemblyCode()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration();

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_valuePropertyTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.True(result.Succeed, $"Error: {result.FailValue?.Code}\nDetails: {result.FailValue?.Details}");
        Assert.NotNull(result.Value);
        Assert.NotNull(result.Value.Result);
        Assert.IsNotEmpty(result.Value.Result);
        Assert.True(result.Value.Result.Contains(_valuePropertyTarget.ClassName)
                    || result.Value.Result.Contains(_valuePropertyTarget.MethodName),
            "Output should contain method or class name");
    }

    [Test]
    public async Task GetJitCodegen_WithMissingMethod_ShouldReturnEmptyOrMinimalOutput()
    {
        // Arrange
        var projectContext = CreateProjectContext();

        var config = new JitDisasmConfiguration();

        // Act
        var result = await _jitCodegenProvider.GetJitCodegenAsync(_missingMethodTarget, projectContext, config, CancellationToken.None);

        // Assert
        Assert.True(result.Succeed, $"Error: {result.FailValue?.Code}\nDetails: {result.FailValue?.Details}");
        Assert.NotNull(result.Value);
        Assert.NotNull(result.Value.Result);
        Assert.That(result.Value.Result.Length, Is.Zero,
            "Expected empty or minimal output for missing method");
    }

    private JitDisasmProjectContext CreateProjectContext([CanBeNull] JitDisasmTargetFramework tfm = null)
    {
        var projectRoot = Path.GetFullPath(Path.Combine(AppDomain.CurrentDomain.BaseDirectory, "..", "..", "..", "..", ".."));
        var dotnetCmd = Path.Combine(projectRoot, "dotnet.cmd");
        var dotnetExe = File.Exists(dotnetCmd) ? dotnetCmd : "dotnet";

        TestContext.WriteLine($"Project root: {projectRoot}");
        TestContext.WriteLine($"Using dotnet executable: {dotnetExe}");
        TestContext.WriteLine($"File exists: {File.Exists(dotnetExe)}");

        return new JitDisasmProjectContext(
            Sdk: "Microsoft.NET.Sdk",
            Tfm: tfm ?? new JitDisasmTargetFramework(
                UniqueString: "net8.0",
                new Version(8, 0, 0, 0),
                IsNetCore: true),
            OutputPath: "bin",
            ProjectFilePath: _testProjectFile,
            ProjectDirectory: _testProjectDir,
            AssemblyName: "TestProject",
            DotNetCliExePath: dotnetExe);
    }
}
