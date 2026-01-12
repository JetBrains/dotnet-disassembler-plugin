package com.jetbrains.rider.plugins.dotnetdisassembler.language

import com.intellij.icons.AllIcons
import com.intellij.openapi.fileTypes.LanguageFileType
import javax.swing.Icon

object AsmFileType : LanguageFileType(AsmLanguage) {
    override fun getName(): String = "JitAsm"
    override fun getDescription(): String = "Jit assembly"
    override fun getDefaultExtension(): String = "jitasm"
    override fun getIcon(): Icon = AllIcons.FileTypes.Text
}
