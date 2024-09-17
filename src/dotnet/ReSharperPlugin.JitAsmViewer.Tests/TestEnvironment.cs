using System.Threading;
using JetBrains.Application.BuildScript.Application.Zones;
using JetBrains.ReSharper.Feature.Services;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.TestFramework;
using JetBrains.TestFramework;
using JetBrains.TestFramework.Application.Zones;
using NUnit.Framework;

[assembly: Apartment(ApartmentState.STA)]

namespace ReSharperPlugin.JitAsmViewer.Tests
{
    [ZoneDefinition]
    public class JitAsmViewerTestEnvironmentZone : ITestsEnvZone, IRequire<PsiFeatureTestZone>, IRequire<IJitAsmViewerZone> { }

    [ZoneMarker]
    public class ZoneMarker : IRequire<ICodeEditingZone>, IRequire<ILanguageCSharpZone>, IRequire<JitAsmViewerTestEnvironmentZone> { }

    [SetUpFixture]
    public class JitAsmViewerTestsAssembly : ExtensionTestEnvironmentAssembly<JitAsmViewerTestEnvironmentZone> { }
}
