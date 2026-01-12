package com.jetbrains.rider.plugins.dotnetdisassembler.language

import com.intellij.lang.Language

object AsmLanguage : Language("JitAsm") {
    private fun readResolve(): Any = AsmLanguage
}
