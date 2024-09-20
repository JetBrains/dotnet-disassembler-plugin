package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope

@Service(Service.Level.PROJECT)
class PluginProjectRoot(val coroutineScope: CoroutineScope) : Disposable.Default {

    companion object {
        fun getInstance(project: Project): PluginProjectRoot {
            return project.service<PluginProjectRoot>()
        }
    }
}

@Service
class PluginAppRoot(val coroutineScope: CoroutineScope) {

    companion object {
        fun getInstance(): PluginAppRoot = service()
    }
}