package com.jetbrains.rider.plugins.jitasmviewer.language

import com.intellij.lang.Language

object AsmLanguage : Language("JitAsm") {
    private fun readResolve(): Any = AsmLanguage
}
