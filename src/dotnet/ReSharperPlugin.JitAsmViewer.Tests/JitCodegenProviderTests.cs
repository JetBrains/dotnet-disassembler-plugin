using System;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.CompilerServices;
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
    protected void DoTest(Func<IPsiSourceFile, int> offset, JitDisasmConfiguration jitDisasmConfiguration, [CallerMemberName] string testFileName = "")
    {
        if (string.IsNullOrWhiteSpace(testFileName))
            throw new TestFailureException("Method name is empty");
        
        DoTestSolution(_solutionName, PrepareSolutionFlags.COPY_TO_TEMP_FOLDER_ONCE | PrepareSolutionFlags.RESTORE_NUGETS, (_, solution) =>
        {
            var testLifetime = new Lifetime();
            var fileName = testFileName.Replace("Test", "");
            var testProject = solution.GetProjectByName(nameof(JitCodegenProviderTests));
            var psiModules = solution.GetComponent<IPsiModules>();
            var psiServices = solution.GetPsiServices();

            var testCsProjectFile = testProject.GetAllProjectFiles(x => Path.GetFileNameWithoutExtension(x.Name) == fileName).Single();
            
            var testPsiSourceFile = psiModules.GetPsiSourceFilesFor(testCsProjectFile).Single();

            var testCsFile = psiServices.Files.GetPsiFiles<KnownLanguage>(testPsiSourceFile, PsiLanguageCategories.All).Single();

            var tokenNode = testCsFile.FindTokenAt(new DocumentOffset(testPsiSourceFile.Document, offset(testPsiSourceFile)));
            if (tokenNode == null)
                throw new TestFailureException("Cannot find token under cursor");

            if (!JitDisasmTargetUtils.ValidateTreeNodeForDisasm(tokenNode.Parent) ||
                tokenNode.Parent is not IDeclaration declaration)
                throw new TestFailureException("Token's parent is not IDeclaration");

            if (declaration.DeclaredElement is not { } declaredElement)
                throw new TestFailureException("declaration.DeclaredElement is null");

            var target = JitDisasmTargetUtils.GetTarget(declaredElement);
            var result = new JitCodegenProvider(testProject).GetJitCodegen(target, jitDisasmConfiguration, testLifetime)
                .Result;

            if (!result.Succeed)
                throw new TestFailureException(result.FailMessage);
            var asmString = result.Value.Result;

            ExecuteWithGold(testPsiSourceFile, sw =>
            {
                sw.WriteLine("DeclaredElemet: {0}", declaredElement);
                sw.Write(asmString);
                sw.WriteLine("#END#");
            });
        });
    }
}

[TestNet80]
public class JitCodegenProviderTestsNetCoreTests : JitCodegenProviderTests
{
    [Test]
    public void TestJitCodegen()
    {
        /*var lifetime = new Lifetime();
        var solution = this.Solution;
        var textControl = this.OpenTextControl(lifetime);
        /*var dataContext = textControl.ToDataContext()(lifetime);
        var elementUnderCaret = dataContext.GetData(PsiDataConstants.SELECTED_TREE_NODES)?.FirstOrDefault();
        if (elementUnderCaret is not { } selectedItem ||
            !JitDisasmTargetUtils.ValidateTreeNodeForDisasm(selectedItem.Parent) ||
            selectedItem.Parent is not IDeclaration declaration)
            return null;

        var result = (await GetJitCodegenForDeclaration(declaration, lifetime)).Value.Result;*/
    }
}

[TestNet80]
public class TestClass : BaseTestWithTextControl
{
    protected override string RelativeTestDataPath => nameof(JitCodegenProviderTests);

    protected override void DoTest(Lifetime lifetime, IProject testProject)
    {
        var textControl = OpenTextControl(lifetime);
    }

    [Test]
    public void TestJitCodegen()
    {
        DoNamedTest2();
        /*var lifetime = new Lifetime();
        var solution = this.Solution;
        var textControl = this.OpenTextControl(lifetime);
        /*var dataContext = textControl.ToDataContext()(lifetime);
        var elementUnderCaret = dataContext.GetData(PsiDataConstants.SELECTED_TREE_NODES)?.FirstOrDefault();
        if (elementUnderCaret is not { } selectedItem ||
            !JitDisasmTargetUtils.ValidateTreeNodeForDisasm(selectedItem.Parent) ||
            selectedItem.Parent is not IDeclaration declaration)
            return null;

        var result = (await GetJitCodegenForDeclaration(declaration, lifetime)).Value.Result;*/
    }
}