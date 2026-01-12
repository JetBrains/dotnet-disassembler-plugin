package com.jetbrains.rider.plugins.dotnetdisassembler

enum class AsmViewerStatus {
    Initializing,
    WaitingForInput,
    Loading,
    Content,
    Unavailable
}
