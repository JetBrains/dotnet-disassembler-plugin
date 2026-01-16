using NUnit.Framework;

namespace ReSharperPlugin.DotNetDisassembler.Tests;

[TestFixture]
public class FusValidationPatternsTests
{
    [TestFixture]
    public class SdkTypeTests
    {
        [TestCase("Microsoft.NET.Sdk", Description = "Base SDK")]
        [TestCase("Microsoft.NET.Sdk.Web", Description = "Web SDK")]
        [TestCase("Microsoft.NET.Sdk.Worker", Description = "Worker SDK")]
        [TestCase("Microsoft.NET.Sdk.Razor", Description = "Razor SDK")]
        [TestCase("Microsoft.NET.Sdk.BlazorWebAssembly", Description = "Blazor WebAssembly SDK")]
        [TestCase("Microsoft.NET.Sdk.WindowsDesktop", Description = "Windows Desktop SDK (.NET Core 3.x)")]
        public void ShouldMatchValidSdkTypes(string sdkType)
        {
            var match = FusValidationPatterns.SdkTypeRegex.Match(sdkType);

            Assert.That(match.Success, Is.True, $"Should match SDK type: {sdkType}");
            Assert.That(match.Value, Is.EqualTo(sdkType), "Should match the entire string");
        }

        [TestCase("Microsoft.NET", Description = "Incomplete SDK name")]
        [TestCase("Microsoft.NET.Sdk.123Number", Description = "SDK extension starting with number")]
        [TestCase("MyCustom.NET.Sdk", Description = "Non-Microsoft SDK")]
        [TestCase("microsoft.net.sdk", Description = "Lowercase")]
        [TestCase("", Description = "Empty string")]
        public void ShouldNotMatchInvalidSdkTypes(string sdkType)
        {
            var match = FusValidationPatterns.SdkTypeRegex.Match(sdkType);

            Assert.That(match.Success && match.Value == sdkType, Is.False,
                $"Should not match invalid SDK type: {sdkType}");
        }
    }

    [TestFixture]
    public class TargetFrameworkTests
    {
        [TestCase("net8.0", Description = ".NET 8")]
        [TestCase("net9.0", Description = ".NET 9")]
        [TestCase("net6.0", Description = ".NET 6")]
        [TestCase("net5.0", Description = ".NET 5")]
        [TestCase("net8.0-windows", Description = ".NET 8 for Windows")]
        [TestCase("net6.0-android", Description = ".NET 6 for Android")]
        [TestCase("net7.0-ios", Description = ".NET 7 for iOS")]
        [TestCase("netcoreapp3.1", Description = ".NET Core 3.1")]
        [TestCase("netcoreapp2.1", Description = ".NET Core 2.1")]
        [TestCase("netcoreapp1.0", Description = ".NET Core 1.0")]
        [TestCase("netstandard2.1", Description = ".NET Standard 2.1")]
        [TestCase("netstandard2.0", Description = ".NET Standard 2.0")]
        [TestCase("netstandard1.6", Description = ".NET Standard 1.6")]
        [TestCase("netstandard1.0", Description = ".NET Standard 1.0")]
        [TestCase("net472", Description = ".NET Framework 4.7.2")]
        [TestCase("net48", Description = ".NET Framework 4.8")]
        [TestCase("net481", Description = ".NET Framework 4.8.1")]
        [TestCase("net462", Description = ".NET Framework 4.6.2")]
        public void ShouldMatchValidTargetFrameworks(string tfm)
        {
            var match = FusValidationPatterns.TargetFrameworkRegex.Match(tfm);

            Assert.That(match.Success, Is.True, $"Should match TFM: {tfm}");
            Assert.That(match.Value, Is.EqualTo(tfm), "Should match the entire string");
        }

        [TestCase("netframework4.8", Description = "Invalid format")]
        [TestCase("dotnet8.0", Description = "Wrong prefix")]
        [TestCase("NET8.0", Description = "Uppercase")]
        [TestCase("net8", Description = "Missing minor version")]
        [TestCase("", Description = "Empty string")]
        [TestCase("net8.0.0", Description = "Too many version components")]
        public void ShouldNotMatchInvalidTargetFrameworks(string tfm)
        {
            var match = FusValidationPatterns.TargetFrameworkRegex.Match(tfm);

            Assert.That(match.Success && match.Value == tfm, Is.False,
                $"Should not match invalid TFM: {tfm}");
        }

        [TestCase("net8.0-windows10.0.19041", Description = "Windows with build number")]
        [TestCase("net6.0-android31.0", Description = "Android with API level")]
        [TestCase("net8.0-ios17.2", Description = "iOS with version (from official docs)")]
        [TestCase("net8.0-android34.0", Description = "Android API level 34 (from official docs)")]
        [TestCase("net6.0-ios15.0", Description = "iOS with default version")]
        [TestCase("net8.0-maccatalyst", Description = "Mac Catalyst")]
        [TestCase("net8.0-tvos", Description = "tvOS")]
        public void ShouldMatchTargetFrameworksWithExtendedPlatformInfo(string tfm)
        {
            var match = FusValidationPatterns.TargetFrameworkRegex.Match(tfm);

            Assert.That(match.Success, Is.True, $"Should match TFM with platform info: {tfm}");
            Assert.That(match.Value, Is.EqualTo(tfm), "Should match the entire string");
        }
    }
}
