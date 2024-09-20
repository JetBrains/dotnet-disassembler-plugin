package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

class JitAsmProviderModel: Ext(SolutionModel.Solution) {
    init {
        call("GetJitCodegenForSelectedElement", void, string.nullable)
    }
}