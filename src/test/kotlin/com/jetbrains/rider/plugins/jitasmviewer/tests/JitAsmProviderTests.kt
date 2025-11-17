package com.jetbrains.rider.plugins.jitasmviewer.tests

import com.jetbrains.rider.test.annotations.TestEnvironment
import com.jetbrains.rider.test.base.BaseTestWithSolution
import com.jetbrains.rider.test.env.enums.SdkVersion
import org.testng.annotations.Test

@TestEnvironment(sdkVersion = SdkVersion.DOT_NET_8)
class JitAsmProviderTests: BaseTestWithSolution() {
    fun getSolutionDirectoryName(): String = "JItCodegenTestSolution"

    @Test
    fun autoAttachChildrenDotNetProjectConfigurationTest() {
        /*val runManager = RunManager.getInstance(project)
        val configuration = runManager.allSettings.first { it.configuration is DotNetProjectConfiguration && it.name == projectName }
        val dotNetProjectConfiguration: DotNetProjectConfiguration = configuration.configuration as DotNetProjectConfiguration
        dotNetProjectConfiguration.parameters.autoAttachToChildren = true
        selectRunConfiguration(project, dotNetProjectConfiguration)
        runAutoAttachChildrenTest()*/
    }
}