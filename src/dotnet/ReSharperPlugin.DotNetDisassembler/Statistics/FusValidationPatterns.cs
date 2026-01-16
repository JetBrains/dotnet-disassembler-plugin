using System.Text.RegularExpressions;

namespace ReSharperPlugin.DotNetDisassembler;

public static class FusValidationPatterns
{
    public const string SdkType = @"Microsoft\.NET\.Sdk(\.[A-Za-z][A-Za-z0-9]*)*";
    public static readonly Regex SdkTypeRegex = new(SdkType, RegexOptions.Compiled);

    public const string TargetFramework = @"(net|netcoreapp|netstandard)\d+\.\d+(-.+)?|net\d{2,3}";
    public static readonly Regex TargetFrameworkRegex = new(TargetFramework, RegexOptions.Compiled);
}
