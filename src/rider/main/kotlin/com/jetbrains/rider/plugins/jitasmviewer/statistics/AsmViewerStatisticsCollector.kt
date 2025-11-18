package com.jetbrains.rider.plugins.jitasmviewer.statistics

import com.intellij.internal.statistic.eventLog.EventLogGroup
import com.intellij.internal.statistic.eventLog.events.EventFields
import com.intellij.internal.statistic.service.fus.collectors.CounterUsagesCollector
import com.intellij.openapi.project.Project

object AsmViewerStatisticsCollector : CounterUsagesCollector() {
    private val GROUP = EventLogGroup("jit.asm.viewer", 1)

    enum class SnapshotAction {
        Created,
        Deleted
    }

    private val SNAPSHOT_ACTION_FIELD = EventFields.Enum<SnapshotAction>("snapshot_action")
    private val DIFFABLE_MODE_FIELD = EventFields.Boolean("diffable_mode")

    private val TOOL_WINDOW_OPENED = GROUP.registerEvent("tool_window.opened", "Tool window opened")
    private val TOOL_WINDOW_CLOSED = GROUP.registerEvent("tool_window.closed", "Tool window closed")

    private val TOOLS_MENU_USED = GROUP.registerEvent("tools_menu.used", "Tools menu used")

    private val SNAPSHOT_ACTION_PERFORMED = GROUP.registerEvent(
        "snapshot.action",
        SNAPSHOT_ACTION_FIELD,
        "Snapshot action performed"
    )

    private val DIFFABLE_MODE_TOGGLED = GROUP.registerEvent(
        "diffable_mode.toggled",
        DIFFABLE_MODE_FIELD,
        "Diffable mode toggled"
    )

    override fun getGroup(): EventLogGroup = GROUP

    fun logToolWindowOpened(project: Project) {
        TOOL_WINDOW_OPENED.log(project)
    }

    fun logToolWindowClosed(project: Project) {
        TOOL_WINDOW_CLOSED.log(project)
    }

    fun logToolsMenuUsed(project: Project) {
        TOOLS_MENU_USED.log(project)
    }

    private val SETTINGS_OPENED = GROUP.registerEvent("settings.opened", "Settings opened")

    fun logSettingsOpened(project: Project) {
        SETTINGS_OPENED.log(project)
    }

    fun logSnapshotCreated(project: Project) {
        SNAPSHOT_ACTION_PERFORMED.log(project, SnapshotAction.Created)
    }

    fun logSnapshotDeleted(project: Project) {
        SNAPSHOT_ACTION_PERFORMED.log(project, SnapshotAction.Deleted)
    }

    fun logDiffableModeToggled(project: Project, enabled: Boolean) {
        DIFFABLE_MODE_TOGGLED.log(project, enabled)
    }
}