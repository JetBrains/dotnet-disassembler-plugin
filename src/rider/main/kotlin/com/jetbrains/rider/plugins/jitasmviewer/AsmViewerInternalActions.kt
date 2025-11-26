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
import com.jetbrains.rd.ide.model.JitConfiguration
import com.jetbrains.rd.ide.model.asmViewerModel
import com.jetbrains.rider.plugins.jitasmviewer.statistics.AsmViewerStatisticsCollector
import com.jetbrains.rider.projectView.solution
import java.awt.BorderLayout
import javax.swing.JPanel

class CreateSnapshotAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.create.snapshot.text"),
    AsmViewerBundle.messagePointer("action.create.snapshot.description"),
    AllIcons.Actions.Dump
) {
    override fun actionPerformed(e: AnActionEvent) {
        val model = project.solution.asmViewerModel
        model.compilationResult.value?.content?.let { content ->
            model.snapshotContent.set(content)
            AsmViewerStatisticsCollector.logSnapshotCreated(project)
        }
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = project.solution.asmViewerModel.compilationResult.value?.content != null
    }
}

class DiffableModeAction(private val project: Project) : ToggleAction(
    AsmViewerBundle.messagePointer("action.diffable.mode.text"),
    AsmViewerBundle.messagePointer("action.diffable.mode.description"),
    AllIcons.Actions.Diff
) {
    override fun isSelected(e: AnActionEvent): Boolean {
        return project.solution.asmViewerModel.configuration.value?.diffable ?: false
    }

    override fun setSelected(e: AnActionEvent, state: Boolean) {
        val model = project.solution.asmViewerModel
        val currentConfig = model.configuration.value
        val newConfig = JitConfiguration(
            showAsmComments = currentConfig?.showAsmComments ?: true,
            diffable = state,
            useTieredJit = currentConfig?.useTieredJit ?: false,
            usePGO = currentConfig?.usePGO ?: false,
            runAppMode = currentConfig?.runAppMode ?: false,
            useNoRestoreFlag = currentConfig?.useNoRestoreFlag ?: false,
            useDotnetPublishForReload = currentConfig?.useDotnetPublishForReload ?: false,
            useDotnetBuildForReload = currentConfig?.useDotnetBuildForReload ?: false,
            useUnloadableContext = currentConfig?.useUnloadableContext ?: false,
            dontGuessTFM = currentConfig?.dontGuessTFM ?: false,
            selectedCustomJit = currentConfig?.selectedCustomJit
        )
        model.configuration.set(newConfig)
        AsmViewerStatisticsCollector.logDiffableModeToggled(project, state)
    }
}

class DeleteSnapshotAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.delete.snapshot.text"),
    AsmViewerBundle.messagePointer("action.delete.snapshot.description"),
    AllIcons.Actions.GC
) {
    override fun actionPerformed(e: AnActionEvent) {
        val model = project.solution.asmViewerModel
        model.snapshotContent.set(null)
        AsmViewerStatisticsCollector.logSnapshotDeleted(project)
    }

    override fun update(e: AnActionEvent) {
        e.presentation.isEnabled = project.solution.asmViewerModel.snapshotContent.value != null
    }
}

class SettingsAction(private val project: Project) : AnAction(
    AsmViewerBundle.messagePointer("action.configuration.text"),
    AsmViewerBundle.messagePointer("action.configuration.description"),
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
