package com.jetbrains.rider.plugins.jitasmviewer.tests

import com.jetbrains.rider.test.annotations.TestSettings
import com.jetbrains.rider.test.base.PerTestSolutionTestBase
import com.jetbrains.rider.test.enums.BuildTool
import com.jetbrains.rider.test.enums.sdk.SdkVersion
import org.testng.annotations.Test

@TestSettings(sdkVersion = SdkVersion.AUTODETECT, buildTool = BuildTool.AUTODETECT)
class JitAsmProviderTests : PerTestSolutionTestBase() {

    @Test
    fun placeholderTest() {
        // TODO: Implement actual integration tests for JIT ASM viewer
        assert(true)
    }
}