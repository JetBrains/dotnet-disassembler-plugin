package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.xmlb.annotations.Tag
import com.jetbrains.rd.ide.model.JitConfiguration

@Tag("jitConfiguration")
class JitConfigurationState : BaseState() {
    var showAsmComments by property(true)
    var diffable by property(true)
    var useTieredJit by property(false)
    var usePGO by property(false)
    var runAppMode by property(false)
    var useNoRestoreFlag by property(false)
    var useDotnetPublishForReload by property(false)
    var useDotnetBuildForReload by property(false)
    var targetFrameworkOverride by string(null)
    var selectedCustomJit by string(null)
    var disassemblyTimeoutSeconds by property(120)
}

@Service(Service.Level.PROJECT)
@State(
    name = "AsmViewerSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class AsmViewerSettings : SimplePersistentStateComponent<AsmViewerSettings.State>(State()) {

    class State : BaseState() {
        var jit by property(JitConfigurationState())
    }

    private val changeListeners = mutableListOf<() -> Unit>()

    companion object {
        fun getInstance(project: Project): AsmViewerSettings = project.service()
    }

    fun addChangeListener(listener: () -> Unit, parentDisposable: Disposable) {
        changeListeners.add(listener)
        Disposer.register(parentDisposable) { changeListeners.remove(listener) }
    }

    private fun fireChangeListeners() {
        changeListeners.forEach { it() }
    }

    fun toJitConfiguration() = JitConfiguration(
        showAsmComments = state.jit.showAsmComments,
        diffable = state.jit.diffable,
        useTieredJit = state.jit.useTieredJit,
        usePGO = state.jit.usePGO,
        runAppMode = state.jit.runAppMode,
        useNoRestoreFlag = state.jit.useNoRestoreFlag,
        useDotnetPublishForReload = state.jit.useDotnetPublishForReload,
        useDotnetBuildForReload = state.jit.useDotnetBuildForReload,
        targetFrameworkOverride = state.jit.targetFrameworkOverride,
        selectedCustomJit = state.jit.selectedCustomJit,
        disassemblyTimeoutSeconds = state.jit.disassemblyTimeoutSeconds
    )

    fun updateFrom(config: JitConfiguration) {
        state.jit.showAsmComments = config.showAsmComments
        state.jit.diffable = config.diffable
        state.jit.useTieredJit = config.useTieredJit
        state.jit.usePGO = config.usePGO
        state.jit.runAppMode = config.runAppMode
        state.jit.useNoRestoreFlag = config.useNoRestoreFlag
        state.jit.useDotnetPublishForReload = config.useDotnetPublishForReload
        state.jit.useDotnetBuildForReload = config.useDotnetBuildForReload
        state.jit.targetFrameworkOverride = config.targetFrameworkOverride
        state.jit.selectedCustomJit = config.selectedCustomJit
        state.jit.disassemblyTimeoutSeconds = config.disassemblyTimeoutSeconds
        fireChangeListeners()
    }
}
