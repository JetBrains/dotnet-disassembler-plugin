package model.rider

import com.jetbrains.rd.generator.nova.Ext
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rd.generator.nova.nullable
import com.jetbrains.rd.generator.nova.property
import com.jetbrains.rd.generator.nova.signal
import com.jetbrains.rider.model.nova.ide.IdeRoot
import com.jetbrains.rider.model.nova.ide.UIAutomationInteractionModel.BeControl

@Suppress("unused")
class JitAsmViewerToolWindow : Ext(IdeRoot) {
    init {
        property("toolWindowContent", BeControl)
        signal("activateToolWindow", bool)

        @Suppress("LocalVariableName")
        val JitAsmViewerToolWindowPanel = classdef("BeJitAsmViewerToolWindowPanel") extends BeControl {
            property("contentToShow", string.nullable)
        }
    }
}