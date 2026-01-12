package com.jetbrains.rider.plugins.dotnetdisassembler

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.jetbrains.rider.plugins.dotnetdisassembler.statistics.AsmViewerStatisticsCollector

class ShowAsmViewerAction : AnAction() {
    override fun actionPerformed(e: AnActionEvent) {
        e.project?.let { project ->
            AsmViewerHostUi.getInstance(project).activateToolwindow()
            AsmViewerStatisticsCollector.logToolsMenuOpened(project)
        }
    }
}

class AsmViewerToolWindowFactory : ToolWindowFactory, DumbAware {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        AsmViewerHostUi.getInstance(project).initializeToolWindow()
    }

    override fun init(toolWindow: ToolWindow) {
        toolWindow.stripeTitle = AsmViewerBundle.message("toolwindow.stripe.title")
    }
}
