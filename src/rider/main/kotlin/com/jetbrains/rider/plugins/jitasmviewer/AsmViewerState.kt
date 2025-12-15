package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rd.ide.model.CompilationResult
import com.jetbrains.rd.util.reactive.IPropertyView
import com.jetbrains.rd.util.reactive.Property

@Service(Service.Level.PROJECT)
class AsmViewerState {

    data class Value(
        val status: AsmViewerStatus,
        val lastResult: CompilationResult?,
        val contentSnapshot: String?
    )

    companion object {
        fun getInstance(project: Project): AsmViewerState = project.service()
    }

    private val _lastResult = Property<CompilationResult?>(null)
    val lastResult: IPropertyView<CompilationResult?> = _lastResult

    private val _status = Property(AsmViewerStatus.Initializing)
    val status: IPropertyView<AsmViewerStatus> = _status

    private val _contentSnapshot = Property<String?>(null)
    val contentSnapshot: IPropertyView<String?> = _contentSnapshot

    fun setStatus(status: AsmViewerStatus) {
        _status.set(status)
    }

    @Synchronized
    fun setResult(result: CompilationResult?, status: AsmViewerStatus) {
        _lastResult.set(result)
        _status.set(status)
    }

    @Synchronized
    fun saveContentSnapshot() {
        _lastResult.value?.content?.let { _contentSnapshot.set(it) }
    }

    fun clearContentSnapshot() {
        _contentSnapshot.set(null)
    }

    val value: Value
        @Synchronized get() = Value(
            status = _status.value,
            lastResult = _lastResult.value,
            contentSnapshot = _contentSnapshot.value
        )
}
