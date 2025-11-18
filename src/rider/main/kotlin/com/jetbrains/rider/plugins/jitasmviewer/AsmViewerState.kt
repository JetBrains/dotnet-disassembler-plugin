package com.jetbrains.rider.plugins.jitasmviewer

sealed class AsmViewerState {
    data object Initializing : AsmViewerState()

    data object WaitingForInput : AsmViewerState()

    data object Loading : AsmViewerState()

    data class Content(
        val currentAsm: String,
        val snapshotAsm: String? = null
    ) : AsmViewerState() {
        val hasSnapshot: Boolean get() = snapshotAsm != null
    }

    data class Unavailable(val reason: String) : AsmViewerState()
}
