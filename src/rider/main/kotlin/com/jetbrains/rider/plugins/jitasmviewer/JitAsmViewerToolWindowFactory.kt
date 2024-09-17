package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.jetbrains.rd.ui.bindable.views.utils.BeControlHost

class JitAsmViewerToolWindowFactory: ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val component = BeControlHost().apply {
            bind(project.lifetime, JitAsmViewerToolWindowHost.getInstance(project).interactionModel.toolWindowContent)
        }
        val content = contentFactory.createContent(component, null, false)
        toolWindow.contentManager.addContent(content)
    }

}