package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.jetbrains.rd.ide.model.asmViewerModel
import com.jetbrains.rider.plugins.jitasmviewer.statistics.AsmViewerStatisticsCollector
import com.jetbrains.rider.projectView.solution
import java.awt.BorderLayout
import javax.swing.JPanel

class CreateSnapshotAction(private val project: Project) : AnAction(
    "Create Snapshot",
    "Save current ASM code as snapshot",
    AllIcons.Actions.Dump
) {
    override fun actionPerformed(e: AnActionEvent) {
        val model = project.solution.asmViewerModel
        model.currentContent.value?.let { content ->
            model.snapshotContent.set(content)
            model.hasSnapshot.set(true)
            AsmViewerStatisticsCollector.logSnapshotCreated(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = project.solution.asmViewerModel.currentContent.value != null
    }
}

class DiffableModeAction(private val project: Project) : ToggleAction(
    "Diffable Mode",
    "Toggle diffable output mode",
    AllIcons.Actions.Diff
) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return project.solution.asmViewerModel.diffable.valueOrNull ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        project.solution.asmViewerModel.diffable.set(state)
        AsmViewerStatisticsCollector.logDiffableModeToggled(project, state)
    }
}

class DeleteSnapshotAction(private val project: Project) : AnAction(
    "Delete Snapshot",
    "Remove snapshot and hide comparison",
    AllIcons.Actions.GC
) {
    override fun actionPerformed(e: AnActionEvent) {
        val model = project.solution.asmViewerModel
        model.snapshotContent.set(null)
        model.hasSnapshot.set(false)
        AsmViewerStatisticsCollector.logSnapshotDeleted(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = project.solution.asmViewerModel.hasSnapshot.valueOrNull ?: false
    }
}

class SettingsAction(private val project: Project) : AnAction(
    "Configuration",
    "Configure ASM Viewer options",
    AllIcons.General.Settings
) {
    override fun actionPerformed(e: AnActionEvent) {
        ConfigurationDialog(project, project.solution.asmViewerModel).show()
        AsmViewerStatisticsCollector.logSettingsOpened(project)
    }
}

object AsmViewerToolbarFactory {
    fun createToolbar(project: Project, targetComponent: JPanel): JPanel {
        val actionGroup = DefaultActionGroup().apply {
            add(CreateSnapshotAction(project))
            add(DeleteSnapshotAction(project))
            add(DiffableModeAction(project))
            add(Separator.create())
            add(SettingsAction(project))
        }

        val toolbar = ActionManager.getInstance()
            .createActionToolbar(ActionPlaces.TOOLBAR, actionGroup, true)
        toolbar.targetComponent = targetComponent

        return JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(toolbar.component, BorderLayout.CENTER)
        }
    }
}
