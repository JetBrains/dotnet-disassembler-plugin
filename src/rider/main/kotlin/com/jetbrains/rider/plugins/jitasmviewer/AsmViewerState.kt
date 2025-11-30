package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.CompilationResponse
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property

@Service(Service.Level.PROJECT)
class AsmViewerState {

    data class Value(
        val status: AsmViewerStatus,
        val lastResponse: CompilationResponse?,
        val contentSnapshot: String?
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

    val value: Value
        @Synchronized get() = Value(
            status = _status.value,
            lastResponse = _lastResponse.value,
            contentSnapshot = _contentSnapshot.value
        )
}
