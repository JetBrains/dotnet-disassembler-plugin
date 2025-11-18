package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
class AsmViewerModel : Ext(SolutionModel.Solution) {
    init {
        sink("show", void)

        property("isVisible", bool)
        property("isLoading", bool)

        property("unavailabilityReason", string.nullable)
        property("errorCode", string.nullable)

        property("currentContent", string.nullable)
        property("sourceFilePath", string.nullable)
        property("caretOffset", int.nullable)

        property("snapshotContent", string.nullable)
        property("hasSnapshot", bool)

        property("showAsmComments", bool)
        property("useTieredJit", bool)
        property("usePGO", bool)
        property("diffable", bool)
        property("runAppMode", bool)
        property("useNoRestoreFlag", bool)
        property("useDotnetPublishForReload", bool)
        property("useDotnetBuildForReload", bool)
        property("useUnloadableContext", bool)
        property("dontGuessTFM", bool)
        property("selectedCustomJit", string.nullable)
    }
}
