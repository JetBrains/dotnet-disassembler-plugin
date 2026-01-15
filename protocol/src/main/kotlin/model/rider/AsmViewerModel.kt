package model.rider

import com.jetbrains.rd.generator.nova.*
import com.jetbrains.rd.generator.nova.PredefinedType.*
import com.jetbrains.rider.model.nova.ide.SolutionModel

@Suppress("unused")
class AsmViewerModel : Ext(SolutionModel.Solution) {

    private val ErrorCode = enum {
        // PSI/Navigation errors
        +"SourceFileNotFound"
        +"PsiSourceFileUnavailable"
        +"UnsupportedLanguage"
        +"InvalidCaretPosition"

        // Configuration errors
        +"PgoNotSupportedForAot"
        +"RunModeNotSupportedForAot"
        +"TieredJitNotSupportedForAot"
        +"FlowgraphsNotSupportedForAot"
        +"FlowgraphsForClassNotSupported"
        +"UnsupportedTargetFramework"
        +"CustomRuntimeRequiresNet7"
        +"GenericMethodsRequireRunMode"

        // Compilation errors
        +"CompilationFailed"
        +"ProjectPathNotFound"
        +"DotnetBuildFailed"
        +"DotnetPublishFailed"
        +"EmptyDisassembly"

        // Runtime/Path errors
        +"DotNetCliNotFound"
        +"RuntimePackNotFound"
        +"CoreClrCheckedNotFound"

        // Other errors
        +"DisassemblyTimeout"
        +"UpdateCancelled"
        +"UnknownError"
    }

    private val ErrorInfo = structdef {
        field("code", ErrorCode)
        field("details", string.nullable)
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
        field("targetFrameworkOverride", string.nullable)
        field("selectedCustomJit", string.nullable)
        field("disassemblyTimeoutSeconds", int)
    }

    private val CompilationResult = structdef {
        field("content", string.nullable)
        field("error", ErrorInfo.nullable)
    }

    init {
        sink("show", void)

        property("isVisible", bool)
        property("isLoading", bool)

        property("configuration", JitConfiguration)

        source("recompile", void)
        source("forceRecompile", void)
        sink("sendResult", CompilationResult)
    }
}
