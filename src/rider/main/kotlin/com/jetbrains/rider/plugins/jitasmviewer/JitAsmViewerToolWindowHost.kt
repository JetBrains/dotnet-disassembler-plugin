package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.JitAsmViewerToolWindow
import com.jetbrains.rd.ide.model.jitAsmViewerToolWindow
import com.jetbrains.rd.platform.util.idea.ProtocolSubscribedProjectComponent
import com.intellij.openapi.wm.ToolWindowManager
import com.jetbrains.rider.protocol.protocol

@Service(Service.Level.PROJECT)
class JitAsmViewerToolWindowHost(project: Project): ProtocolSubscribedProjectComponent(project) {
    companion object {

        fun getInstance(project: Project): JitAsmViewerToolWindowHost {
            return project.service()
        }
    }

    public val interactionModel: JitAsmViewerToolWindow = project.protocol.jitAsmViewerToolWindow

    init {
        interactionModel.activateToolWindow.advise(projectComponentLifetime) {
            if (!it) return@advise

            val toolWindowManager = ToolWindowManager.getInstance(project)
            toolWindowManager.getToolWindow("CefToolWindow")!!.show()
        }
    }
}