@file:Suppress("EXPERIMENTAL_API_USAGE","EXPERIMENTAL_UNSIGNED_LITERALS","PackageDirectoryMismatch","UnusedImport","unused","LocalVariableName","CanBeVal","PropertyName","EnumEntryName","ClassName","ObjectPropertyName","UnnecessaryVariable","SpellCheckingInspection")
package com.jetbrains.rd.ide.model

import com.jetbrains.rd.framework.*
import com.jetbrains.rd.framework.base.*
import com.jetbrains.rd.framework.impl.*

import com.jetbrains.rd.util.lifetime.*
import com.jetbrains.rd.util.reactive.*
import com.jetbrains.rd.util.string.*
import com.jetbrains.rd.util.*
import kotlin.time.Duration
import kotlin.reflect.KClass
import kotlin.jvm.JvmStatic



/**
 * #### Generated from [AsmViewerModel.kt:8]
 */
class AsmViewerModel private constructor(
    private val _show: RdSignal<Unit>,
    private val _isVisible: RdOptionalProperty<Boolean>,
    private val _isLoading: RdOptionalProperty<Boolean>,
    private val _compile: RdCall<CompileRequest, CompilationResponse>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(564443201287490), classLoader, "com.jetbrains.rd.ide.model.ErrorCode"))
            serializers.register(LazyCompanionMarshaller(RdId(564443201465347), classLoader, "com.jetbrains.rd.ide.model.ErrorInfo"))
            serializers.register(LazyCompanionMarshaller(RdId(2758783625201768569), classLoader, "com.jetbrains.rd.ide.model.CaretPosition"))
            serializers.register(LazyCompanionMarshaller(RdId(3780635925811704340), classLoader, "com.jetbrains.rd.ide.model.JitConfiguration"))
            serializers.register(LazyCompanionMarshaller(RdId(4197557252996108239), classLoader, "com.jetbrains.rd.ide.model.CompileRequest"))
            serializers.register(LazyCompanionMarshaller(RdId(3283825401676346385), classLoader, "com.jetbrains.rd.ide.model.CompilationResponse"))
        }
        
        
        
        
        
        const val serializationHash = -4417874353733812450L
        
    }
    override val serializersOwner: ISerializersOwner get() = AsmViewerModel
    override val serializationHash: Long get() = AsmViewerModel.serializationHash
    
    //fields
    val show: ISource<Unit> get() = _show
    val isVisible: IOptProperty<Boolean> get() = _isVisible
    val isLoading: IOptProperty<Boolean> get() = _isLoading
    val compile: IRdCall<CompileRequest, CompilationResponse> get() = _compile
    //methods
    //initializer
    init {
        _isVisible.optimizeNested = true
        _isLoading.optimizeNested = true
    }
    
    init {
        bindableChildren.add("show" to _show)
        bindableChildren.add("isVisible" to _isVisible)
        bindableChildren.add("isLoading" to _isLoading)
        bindableChildren.add("compile" to _compile)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdCall<CompileRequest, CompilationResponse>(CompileRequest, CompilationResponse)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("AsmViewerModel (")
        printer.indent {
            print("show = "); _show.print(printer); println()
            print("isVisible = "); _isVisible.print(printer); println()
            print("isLoading = "); _isLoading.print(printer); println()
            print("compile = "); _compile.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AsmViewerModel   {
        return AsmViewerModel(
            _show.deepClonePolymorphic(),
            _isVisible.deepClonePolymorphic(),
            _isLoading.deepClonePolymorphic(),
            _compile.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val Solution.asmViewerModel get() = getOrCreateExtension("asmViewerModel", ::AsmViewerModel)



/**
 * #### Generated from [AsmViewerModel.kt:48]
 */
data class CaretPosition (
    val filePath: String,
    val offset: Int,
    val documentModificationStamp: Long
) : IPrintable {
    //companion
    
    companion object : IMarshaller<CaretPosition> {
        override val _type: KClass<CaretPosition> = CaretPosition::class
        override val id: RdId get() = RdId(2758783625201768569)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): CaretPosition  {
            val filePath = buffer.readString()
            val offset = buffer.readInt()
            val documentModificationStamp = buffer.readLong()
            return CaretPosition(filePath, offset, documentModificationStamp)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: CaretPosition)  {
            buffer.writeString(value.filePath)
            buffer.writeInt(value.offset)
            buffer.writeLong(value.documentModificationStamp)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as CaretPosition
        
        if (filePath != other.filePath) return false
        if (offset != other.offset) return false
        if (documentModificationStamp != other.documentModificationStamp) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + filePath.hashCode()
        __r = __r*31 + offset.hashCode()
        __r = __r*31 + documentModificationStamp.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CaretPosition (")
        printer.indent {
            print("filePath = "); filePath.print(printer); println()
            print("offset = "); offset.print(printer); println()
            print("documentModificationStamp = "); documentModificationStamp.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AsmViewerModel.kt:73]
 */
data class CompilationResponse (
    val content: String?,
    val error: ErrorInfo?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<CompilationResponse> {
        override val _type: KClass<CompilationResponse> = CompilationResponse::class
        override val id: RdId get() = RdId(3283825401676346385)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): CompilationResponse  {
            val content = buffer.readNullable { buffer.readString() }
            val error = buffer.readNullable { ErrorInfo.read(ctx, buffer) }
            return CompilationResponse(content, error)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: CompilationResponse)  {
            buffer.writeNullable(value.content) { buffer.writeString(it) }
            buffer.writeNullable(value.error) { ErrorInfo.write(ctx, buffer, it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as CompilationResponse
        
        if (content != other.content) return false
        if (error != other.error) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + if (content != null) content.hashCode() else 0
        __r = __r*31 + if (error != null) error.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CompilationResponse (")
        printer.indent {
            print("content = "); content.print(printer); println()
            print("error = "); error.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AsmViewerModel.kt:68]
 */
data class CompileRequest (
    val caretPosition: CaretPosition,
    val configuration: JitConfiguration
) : IPrintable {
    //companion
    
    companion object : IMarshaller<CompileRequest> {
        override val _type: KClass<CompileRequest> = CompileRequest::class
        override val id: RdId get() = RdId(4197557252996108239)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): CompileRequest  {
            val caretPosition = CaretPosition.read(ctx, buffer)
            val configuration = JitConfiguration.read(ctx, buffer)
            return CompileRequest(caretPosition, configuration)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: CompileRequest)  {
            CaretPosition.write(ctx, buffer, value.caretPosition)
            JitConfiguration.write(ctx, buffer, value.configuration)
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as CompileRequest
        
        if (caretPosition != other.caretPosition) return false
        if (configuration != other.configuration) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + caretPosition.hashCode()
        __r = __r*31 + configuration.hashCode()
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("CompileRequest (")
        printer.indent {
            print("caretPosition = "); caretPosition.print(printer); println()
            print("configuration = "); configuration.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AsmViewerModel.kt:10]
 */
enum class ErrorCode {
    SourceFileNotFound, 
    PsiSourceFileUnavailable, 
    UnsupportedLanguage, 
    InvalidCaretPosition, 
    PgoNotSupportedForAot, 
    RunModeNotSupportedForAot, 
    TieredJitNotSupportedForAot, 
    FlowgraphsNotSupportedForAot, 
    FlowgraphsForClassNotSupported, 
    UnsupportedTargetFramework, 
    CustomRuntimeRequiresNet7, 
    DisassemblyTargetNotFound, 
    CompilationFailed, 
    ProjectPathNotFound, 
    DotnetBuildFailed, 
    DotnetPublishFailed, 
    RuntimePackNotFound, 
    CoreClrCheckedNotFound, 
    ClrJitNotFound, 
    UpdateCancelled, 
    UnknownError;
    
    companion object : IMarshaller<ErrorCode> {
        val marshaller = FrameworkMarshallers.enum<ErrorCode>()
        
        
        override val _type: KClass<ErrorCode> = ErrorCode::class
        override val id: RdId get() = RdId(564443201287490)
        
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ErrorCode {
            return marshaller.read(ctx, buffer)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ErrorCode)  {
            marshaller.write(ctx, buffer, value)
        }
    }
}


/**
 * #### Generated from [AsmViewerModel.kt:43]
 */
data class ErrorInfo (
    val code: ErrorCode,
    val details: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<ErrorInfo> {
        override val _type: KClass<ErrorInfo> = ErrorInfo::class
        override val id: RdId get() = RdId(564443201465347)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): ErrorInfo  {
            val code = buffer.readEnum<ErrorCode>()
            val details = buffer.readNullable { buffer.readString() }
            return ErrorInfo(code, details)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: ErrorInfo)  {
            buffer.writeEnum(value.code)
            buffer.writeNullable(value.details) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as ErrorInfo
        
        if (code != other.code) return false
        if (details != other.details) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + code.hashCode()
        __r = __r*31 + if (details != null) details.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("ErrorInfo (")
        printer.indent {
            print("code = "); code.print(printer); println()
            print("details = "); details.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}


/**
 * #### Generated from [AsmViewerModel.kt:54]
 */
data class JitConfiguration (
    val showAsmComments: Boolean,
    val diffable: Boolean,
    val useTieredJit: Boolean,
    val usePGO: Boolean,
    val runAppMode: Boolean,
    val useNoRestoreFlag: Boolean,
    val useDotnetPublishForReload: Boolean,
    val useDotnetBuildForReload: Boolean,
    val useUnloadableContext: Boolean,
    val dontGuessTFM: Boolean,
    val selectedCustomJit: String?
) : IPrintable {
    //companion
    
    companion object : IMarshaller<JitConfiguration> {
        override val _type: KClass<JitConfiguration> = JitConfiguration::class
        override val id: RdId get() = RdId(3780635925811704340)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): JitConfiguration  {
            val showAsmComments = buffer.readBool()
            val diffable = buffer.readBool()
            val useTieredJit = buffer.readBool()
            val usePGO = buffer.readBool()
            val runAppMode = buffer.readBool()
            val useNoRestoreFlag = buffer.readBool()
            val useDotnetPublishForReload = buffer.readBool()
            val useDotnetBuildForReload = buffer.readBool()
            val useUnloadableContext = buffer.readBool()
            val dontGuessTFM = buffer.readBool()
            val selectedCustomJit = buffer.readNullable { buffer.readString() }
            return JitConfiguration(showAsmComments, diffable, useTieredJit, usePGO, runAppMode, useNoRestoreFlag, useDotnetPublishForReload, useDotnetBuildForReload, useUnloadableContext, dontGuessTFM, selectedCustomJit)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: JitConfiguration)  {
            buffer.writeBool(value.showAsmComments)
            buffer.writeBool(value.diffable)
            buffer.writeBool(value.useTieredJit)
            buffer.writeBool(value.usePGO)
            buffer.writeBool(value.runAppMode)
            buffer.writeBool(value.useNoRestoreFlag)
            buffer.writeBool(value.useDotnetPublishForReload)
            buffer.writeBool(value.useDotnetBuildForReload)
            buffer.writeBool(value.useUnloadableContext)
            buffer.writeBool(value.dontGuessTFM)
            buffer.writeNullable(value.selectedCustomJit) { buffer.writeString(it) }
        }
        
        
    }
    //fields
    //methods
    //initializer
    //secondary constructor
    //equals trait
    override fun equals(other: Any?): Boolean  {
        if (this === other) return true
        if (other == null || other::class != this::class) return false
        
        other as JitConfiguration
        
        if (showAsmComments != other.showAsmComments) return false
        if (diffable != other.diffable) return false
        if (useTieredJit != other.useTieredJit) return false
        if (usePGO != other.usePGO) return false
        if (runAppMode != other.runAppMode) return false
        if (useNoRestoreFlag != other.useNoRestoreFlag) return false
        if (useDotnetPublishForReload != other.useDotnetPublishForReload) return false
        if (useDotnetBuildForReload != other.useDotnetBuildForReload) return false
        if (useUnloadableContext != other.useUnloadableContext) return false
        if (dontGuessTFM != other.dontGuessTFM) return false
        if (selectedCustomJit != other.selectedCustomJit) return false
        
        return true
    }
    //hash code trait
    override fun hashCode(): Int  {
        var __r = 0
        __r = __r*31 + showAsmComments.hashCode()
        __r = __r*31 + diffable.hashCode()
        __r = __r*31 + useTieredJit.hashCode()
        __r = __r*31 + usePGO.hashCode()
        __r = __r*31 + runAppMode.hashCode()
        __r = __r*31 + useNoRestoreFlag.hashCode()
        __r = __r*31 + useDotnetPublishForReload.hashCode()
        __r = __r*31 + useDotnetBuildForReload.hashCode()
        __r = __r*31 + useUnloadableContext.hashCode()
        __r = __r*31 + dontGuessTFM.hashCode()
        __r = __r*31 + if (selectedCustomJit != null) selectedCustomJit.hashCode() else 0
        return __r
    }
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("JitConfiguration (")
        printer.indent {
            print("showAsmComments = "); showAsmComments.print(printer); println()
            print("diffable = "); diffable.print(printer); println()
            print("useTieredJit = "); useTieredJit.print(printer); println()
            print("usePGO = "); usePGO.print(printer); println()
            print("runAppMode = "); runAppMode.print(printer); println()
            print("useNoRestoreFlag = "); useNoRestoreFlag.print(printer); println()
            print("useDotnetPublishForReload = "); useDotnetPublishForReload.print(printer); println()
            print("useDotnetBuildForReload = "); useDotnetBuildForReload.print(printer); println()
            print("useUnloadableContext = "); useUnloadableContext.print(printer); println()
            print("dontGuessTFM = "); dontGuessTFM.print(printer); println()
            print("selectedCustomJit = "); selectedCustomJit.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    //contexts
    //threading
}
