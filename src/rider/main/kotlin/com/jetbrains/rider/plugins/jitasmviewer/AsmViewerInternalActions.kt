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
import com.jetbrains.rider.plugins.jitasmviewer.statistics.AsmViewerStatisticsCollector
import java.awt.BorderLayout
import javax.swing.JPanel

class CreateSnapshotAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.create.snapshot.text"),
    AsmViewerBundle.messagePointer("action.create.snapshot.description"),
    AllIcons.Actions.Dump
) {
    override fun actionPerformed(e: AnActionEvent) {
        AsmViewerState.getInstance(project).saveContentSnapshot()
        AsmViewerStatisticsCollector.logSnapshotCreated(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = AsmViewerState.getInstance(project).lastResponse.value?.content != null
    }
}

class DiffableModeAction(private val project: Project) : ToggleAction(
    AsmViewerBundle.messagePointer("action.diffable.mode.text"),
    AsmViewerBundle.messagePointer("action.diffable.mode.description"),
    AllIcons.Actions.Diff
) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return AsmViewerSettings.getInstance(project).state.jit.diffable
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val settings = AsmViewerSettings.getInstance(project)
        settings.updateFrom(settings.toJitConfiguration().copy(diffable = state))
        AsmViewerStatisticsCollector.logDiffableModeToggled(project, state)
    }
}

class DeleteSnapshotAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.delete.snapshot.text"),
    AsmViewerBundle.messagePointer("action.delete.snapshot.description"),
    AllIcons.Actions.GC
) {
    override fun actionPerformed(e: AnActionEvent) {
        AsmViewerState.getInstance(project).clearContentSnapshot()
        AsmViewerStatisticsCollector.logSnapshotDeleted(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = AsmViewerState.getInstance(project).contentSnapshot.value != null
    }
}

class SettingsAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.configuration.text"),
    AsmViewerBundle.messagePointer("action.configuration.description"),
    AllIcons.General.Settings
) {
    override fun actionPerformed(e: AnActionEvent) {
        ConfigurationDialog(project).show()
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
