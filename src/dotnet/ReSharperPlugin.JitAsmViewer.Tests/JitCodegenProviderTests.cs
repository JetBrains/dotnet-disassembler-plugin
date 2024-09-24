using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
using JetBrains.Annotations;
using JetBrains.DocumentManagers;
using JetBrains.DocumentModel;
using JetBrains.Lifetimes;
using JetBrains.ProjectModel;
using JetBrains.ProjectModel.Search;
using JetBrains.ReSharper.Feature.Services.CSharp.CodeCompletion;
using JetBrains.ReSharper.Feature.Services.Util;
using JetBrains.ReSharper.FeaturesTestFramework.Intentions;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.Cpp.Util;
using JetBrains.ReSharper.Psi.DataContext;
using JetBrains.ReSharper.Psi.Files;
using JetBrains.ReSharper.Psi.Modules;
using JetBrains.ReSharper.Psi.Tree;
using JetBrains.TestFramework.Projects;
using JetBrains.TestFramework.Utils;
using JetBrains.TextControl;
using JetBrains.TextControl.DataContext;
using JetBrains.Util;
using NUnit.Framework;
using ReSharperPlugin.JitAsmViewer.JitDisasm;

namespace ReSharperPlugin.JitAsmViewer.Tests;

using JetBrains.ReSharper.TestFramework;

public abstract class JitCodegenProviderTests : BaseTestWithExistingSolutionLoadedByMsbuild
{
    private readonly string _solutionName = "JitCodegenProviderTests.sln";
    protected override string RelativeTestDataPath => nameof(JitCodegenProviderTests);

    protected override string GetGoldTestDataPath(string fileName)
    {
        return base.GetGoldTestDataPath(fileName);
    }

    protected void DoCodegenTest(Func<IPsiSourceFile, int> offset, JitDisasmConfiguration jitDisasmConfiguration,
        [NotNull] [CallerMemberName] string testFileName = "", [NotNull] [CallerMemberName] string testMethodName = "")
    {
        if (string.IsNullOrWhiteSpace(testFileName))
            throw new TestFailureException("Method name is empty");

        DoTestSolution(_solutionName,
            PrepareSolutionFlags.COPY_TO_TEMP_FOLDER_ONCE | PrepareSolutionFlags.RESTORE_NUGETS, (_, solution) =>
            {
                var testLifetime = new Lifetime();
                var fileName = testFileName.Replace("Test", "");
                var testProject = solution.GetProjectByName(nameof(JitCodegenProviderTests));
                var psiModules = solution.GetComponent<IPsiModules>();
                var psiServices = solution.GetPsiServices();

                var testCsProjectFile = testProject
                    .GetAllProjectFiles(x => Path.GetFileNameWithoutExtension(x.Name) == fileName).Single();

                var testPsiSourceFile = psiModules.GetPsiSourceFilesFor(testCsProjectFile).Single();

                var testCsFile = psiServices.Files
                    .GetPsiFiles<KnownLanguage>(testPsiSourceFile, PsiLanguageCategories.All).Single();

                var tokenNode =
                    testCsFile.FindTokenAt(new DocumentOffset(testPsiSourceFile.Document, offset(testPsiSourceFile)));
                if (tokenNode == null)
                    throw new TestFailureException("Cannot find token under cursor");

                if (!JitDisasmTargetUtils.ValidateTreeNodeForDisasm(tokenNode.Parent) ||
                    tokenNode.Parent is not IDeclaration declaration)
                    throw new TestFailureException("Token's parent is not IDeclaration");

                if (declaration.DeclaredElement is not { } declaredElement)
                    throw new TestFailureException("declaration.DeclaredElement is null");

                var target = JitDisasmTargetUtils.GetTarget(declaredElement);
                var result = new JitCodegenProvider(testProject)
                    .GetJitCodegen(target, jitDisasmConfiguration, testLifetime)
                    .Result;

                if (!result.Succeed)
                    throw new TestFailureException(result.FailMessage);
                var asmString = result.Value.Result;
                
                var goldFileName = Path.GetFileName(testCsProjectFile.Location.FullPath);
                if(!string.IsNullOrWhiteSpace(testMethodName))
                    goldFileName = $"{goldFileName}.{testMethodName.Replace("Test", "")}";
                
                ExecuteWithGold(goldFileName, sw =>
                {
                    sw.WriteLine("DeclaredElemet: {0}", declaredElement);
                    sw.WriteLine(asmString);
                    sw.WriteLine("#END#");
                });
            });
    }
}

[TestNet70]
public class JitCodegenProviderTestsNetCoreTests : JitCodegenProviderTests
{
    // Test JitCodegen.cs
    private void TestJitCodegen(Func<IPsiSourceFile, int> offsetSelector, JitDisasmConfiguration jitDisasmConfiguration, [NotNull] [CallerMemberName] string testMethodName = "")
    {
        DoCodegenTest(offsetSelector, jitDisasmConfiguration, testMethodName: testMethodName);
    }

    [Test]
    public void TestLocalMethod()
    {
        TestJitCodegen(x => x.Document.GetText().IndexOf("LocalAdd(int", StringComparison.InvariantCulture), new JitDisasmConfiguration());
    }
    
    [Test]
    public void TestClass()
    {
        TestJitCodegen(x => x.Document.GetText().IndexOf("TestClass", StringComparison.InvariantCulture), new JitDisasmConfiguration());
    }
}