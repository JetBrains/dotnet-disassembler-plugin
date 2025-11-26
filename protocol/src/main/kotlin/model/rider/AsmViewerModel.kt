package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
class AsmViewerModel : Ext(SolutionModel.Solution) {

    private val ErrorInfo = structdef {
        field("code", string)
        field("details", string.nullable)
    }

    private val CaretPosition = structdef {
        field("filePath", string)
        field("offset", int)
        field("documentModificationStamp", long)
    }

    private val CompilationResult = structdef {
        field("content", string.nullable)
        field("error", ErrorInfo.nullable)
    }

    private val JitConfiguration = structdef {
        field("showAsmComments", bool)
        field("diffable", bool)
        field("useTieredJit", bool)
        field("usePGO", bool)
        field("runAppMode", bool)
        field("useNoRestoreFlag", bool)
        field("useDotnetPublishForReload", bool)
        field("useDotnetBuildForReload", bool)
        field("useUnloadableContext", bool)
        field("dontGuessTFM", bool)
        field("selectedCustomJit", string.nullable)
    }

    init {
        sink("show", void)

        property("isVisible", bool)

        property("caretPosition", CaretPosition.nullable)
        property("compilationResult", CompilationResult.nullable)

        property("snapshotContent", string.nullable)

        property("configuration", JitConfiguration.nullable)
    }
}
