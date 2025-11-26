package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

class AsmViewerConfigurationBundle : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "messages.AsmViewerConfigurationBundle"
        private val INSTANCE = AsmViewerConfigurationBundle()

        @Nls
        fun message(
            @PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any
        ): String = INSTANCE.getMessage(key, *params)
    }
}
