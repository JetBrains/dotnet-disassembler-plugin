package com.jetbrains.rider.plugins.jitasmviewer

import com.intellij.DynamicBundle
import com.jetbrains.rd.ide.model.ErrorCode
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

class AsmViewerBundle : DynamicBundle(BUNDLE) {
    companion object {
        @NonNls
        private const val BUNDLE = "messages.AsmViewerBundle"
        private val INSTANCE = AsmViewerBundle()

        @Nls
        fun message(
            @PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any
        ): String = INSTANCE.getMessage(key, *params)

        fun messagePointer(
            @PropertyKey(resourceBundle = BUNDLE) key: String,
            vararg params: Any
        ): Supplier<String> = INSTANCE.getLazyMessage(key, *params)

        @Nls
        fun errorMessage(errorCode: ErrorCode?, details: String?): String {
            if (errorCode == null) {
                return INSTANCE.getMessage("error.UnknownError")
            }

            val key = "error.$errorCode"
            val localizedMessage = if (INSTANCE.containsKey(key)) {
                INSTANCE.getMessage(key)
            } else {
                INSTANCE.getMessage("error.UnknownError")
            }

            return if (details.isNullOrEmpty()) {
                localizedMessage
            } else {
                "$localizedMessage:\n$details"
            }
        }
    }
}
