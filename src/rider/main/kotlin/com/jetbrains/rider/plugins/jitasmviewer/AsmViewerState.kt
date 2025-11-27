package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.CompilationResponse
import com.jetbrains.rd.ide.model.JitConfiguration
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property

@Service(Service.Level.PROJECT)
class AsmViewerState : LifetimedService() {

    data class Value(
        val status: AsmViewerStatus,
        val lastResponse: CompilationResponse?,
        val contentSnapshot: String?,
        val configuration: JitConfiguration
    )

    companion object {
        fun getInstance(project: Project): AsmViewerState = project.service()
    }

    private val _lastResponse = Property<CompilationResponse?>(null)
    val lastResponse: IPropertyView<CompilationResponse?> = _lastResponse

    private val _status = Property(AsmViewerStatus.WaitingForInput)
    val status: IPropertyView<AsmViewerStatus> = _status

    private val _contentSnapshot = Property<String?>(null)
    val contentSnapshot: IPropertyView<String?> = _contentSnapshot

    private val _configuration = Property(JitConfiguration(
        showAsmComments = true,
        diffable = false,
        useTieredJit = false,
        usePGO = false,
        runAppMode = false,
        useNoRestoreFlag = false,
        useDotnetPublishForReload = false,
        useDotnetBuildForReload = false,
        useUnloadableContext = false,
        dontGuessTFM = false,
        selectedCustomJit = null
    ))
    val configuration: IPropertyView<JitConfiguration> = _configuration

    fun setStatus(status: AsmViewerStatus) {
        _status.set(status)
    }

    @Synchronized
    fun setResponse(response: CompilationResponse?, status: AsmViewerStatus) {
        _lastResponse.set(response)
        _status.set(status)
    }

    @Synchronized
    fun saveContentSnapshot() {
        _lastResponse.value?.content?.let { _contentSnapshot.set(it) }
    }

    fun clearContentSnapshot() {
        _contentSnapshot.set(null)
    }

    @Synchronized
    fun updateConfiguration(transform: (JitConfiguration) -> JitConfiguration) {
        _configuration.set(transform(_configuration.value))
    }

    val value: Value
        @Synchronized get() = Value(
            status = _status.value,
            lastResponse = _lastResponse.value,
            contentSnapshot = _contentSnapshot.value,
            configuration = _configuration.value
        )
}
