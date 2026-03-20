package com.jetbrains.rider.plugins.dotnetdisassembler

import com.intellij.DynamicBundle
import com.jetbrains.rd.ide.model.ErrorCode
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

internal object AsmViewerBundle {
    @NonNls
    private const val BUNDLE: String = "messages.AsmViewerBundle"

    private val INSTANCE = DynamicBundle(AsmViewerBundle::class.java, BUNDLE)

    @Nls
    fun message(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): String =
        INSTANCE.getMessage(key, *params)

    fun messagePointer(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): Supplier<String> =
        INSTANCE.getLazyMessage(key, *params)

    @Nls
    fun errorMessage(errorCode: ErrorCode?): String {
        if (errorCode == null) {
            return message("error.UnknownError")
        }
        val key = "error.$errorCode"
        return if (INSTANCE.containsKey(key)) {
            message(key)
        } else {
            message("error.UnknownError")
        }
    }
}
