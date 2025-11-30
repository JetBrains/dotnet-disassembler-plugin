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

        // Compilation errors
        +"DisassemblyTargetNotFound"
        +"CompilationFailed"
        +"ProjectPathNotFound"
        +"DotnetBuildFailed"
        +"DotnetPublishFailed"

        // Runtime/Path errors
        +"RuntimePackNotFound"
        +"CoreClrCheckedNotFound"
        +"ClrJitNotFound"

        // Other errors
        +"UpdateCancelled"
        +"UnknownError"
    }

    private val ErrorInfo = structdef {
        field("code", ErrorCode)
        field("details", string.nullable)
    }

    private val CaretPosition = structdef {
        field("filePath", string)
        field("fileStamp", long)
        field("offset", int)
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

    private val CompileRequest = structdef {
        field("caretPosition", CaretPosition)
        field("configuration", JitConfiguration)
    }

    private val CompilationResponse = structdef {
        field("content", string.nullable)
        field("error", ErrorInfo.nullable)
    }

    init {
        sink("show", void)

        property("isVisible", bool)
        property("isLoading", bool)

        call("compile", CompileRequest, CompilationResponse)
    }
}
