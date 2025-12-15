package com.jetbrains.rider.plugins.jitasmviewer

enum class AsmViewerStatus {
    Initializing,
    WaitingForInput,
    Loading,
    Content,
    Unavailable
}
