package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.xmlb.annotations.Tag
import com.jetbrains.rd.ide.model.JitConfiguration

@Tag("jit")
class JitState : BaseState() {
    var showAsmComments by property(true)
    var diffable by property(false)
    var useTieredJit by property(false)
    var usePGO by property(false)
    var runAppMode by property(false)
    var useNoRestoreFlag by property(false)
    var useDotnetPublishForReload by property(false)
    var useDotnetBuildForReload by property(false)
    var useUnloadableContext by property(false)
    var dontGuessTFM by property(false)
    var selectedCustomJit by string(null)
}

@Service(Service.Level.PROJECT)
@State(
    name = "AsmViewerSettings",
    storages = [Storage(StoragePathMacros.WORKSPACE_FILE)]
)
class AsmViewerSettings : SimplePersistentStateComponent<AsmViewerSettings.State>(State()) {

    class State : BaseState() {
        var jit by property(JitState())
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
        useUnloadableContext = state.jit.useUnloadableContext,
        dontGuessTFM = state.jit.dontGuessTFM,
        selectedCustomJit = state.jit.selectedCustomJit
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
        state.jit.useUnloadableContext = config.useUnloadableContext
        state.jit.dontGuessTFM = config.dontGuessTFM
        state.jit.selectedCustomJit = config.selectedCustomJit
        fireChangeListeners()
    }
}
