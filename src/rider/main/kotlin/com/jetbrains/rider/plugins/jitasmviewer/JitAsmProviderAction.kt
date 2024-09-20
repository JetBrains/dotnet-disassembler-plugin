package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rd.ide.model.jitAsmProviderModel
import com.jetbrains.rd.util.lifetime.Lifetime
import kotlinx.coroutines.launch


class JitAsmProviderAction: AnAction() {
    override fun actionPerformed(event: AnActionEvent) {
        if(event.project == null)
            return
        PluginProjectRoot.getInstance(event.project!!).coroutineScope.launch {
            val resul = event.project!!.solution.jitAsmProviderModel.getJitCodegenForSelectedElement.startSuspending(Unit)
        }
    }
}