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
    private val _unavailabilityReason: RdProperty<String?>,
    private val _errorCode: RdProperty<String?>,
    private val _currentContent: RdProperty<String?>,
    private val _sourceFilePath: RdProperty<String?>,
    private val _caretOffset: RdProperty<Int?>,
    private val _documentModificationStamp: RdProperty<Long?>,
    private val _snapshotContent: RdProperty<String?>,
    private val _hasSnapshot: RdOptionalProperty<Boolean>,
    private val _showAsmComments: RdOptionalProperty<Boolean>,
    private val _useTieredJit: RdOptionalProperty<Boolean>,
    private val _usePGO: RdOptionalProperty<Boolean>,
    private val _diffable: RdOptionalProperty<Boolean>,
    private val _runAppMode: RdOptionalProperty<Boolean>,
    private val _useNoRestoreFlag: RdOptionalProperty<Boolean>,
    private val _useDotnetPublishForReload: RdOptionalProperty<Boolean>,
    private val _useDotnetBuildForReload: RdOptionalProperty<Boolean>,
    private val _useUnloadableContext: RdOptionalProperty<Boolean>,
    private val _dontGuessTFM: RdOptionalProperty<Boolean>,
    private val _selectedCustomJit: RdProperty<String?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
        }
        
        
        
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        private val __IntNullableSerializer = FrameworkMarshallers.Int.nullable()
        private val __LongNullableSerializer = FrameworkMarshallers.Long.nullable()
        
        const val serializationHash = 8782978450809573702L
        
    }
    override val serializersOwner: ISerializersOwner get() = AsmViewerModel
    override val serializationHash: Long get() = AsmViewerModel.serializationHash
    
    //fields
    val show: ISource<Unit> get() = _show
    val isVisible: IOptProperty<Boolean> get() = _isVisible
    val isLoading: IOptProperty<Boolean> get() = _isLoading
    val unavailabilityReason: IProperty<String?> get() = _unavailabilityReason
    val errorCode: IProperty<String?> get() = _errorCode
    val currentContent: IProperty<String?> get() = _currentContent
    val sourceFilePath: IProperty<String?> get() = _sourceFilePath
    val caretOffset: IProperty<Int?> get() = _caretOffset
    val documentModificationStamp: IProperty<Long?> get() = _documentModificationStamp
    val snapshotContent: IProperty<String?> get() = _snapshotContent
    val hasSnapshot: IOptProperty<Boolean> get() = _hasSnapshot
    val showAsmComments: IOptProperty<Boolean> get() = _showAsmComments
    val useTieredJit: IOptProperty<Boolean> get() = _useTieredJit
    val usePGO: IOptProperty<Boolean> get() = _usePGO
    val diffable: IOptProperty<Boolean> get() = _diffable
    val runAppMode: IOptProperty<Boolean> get() = _runAppMode
    val useNoRestoreFlag: IOptProperty<Boolean> get() = _useNoRestoreFlag
    val useDotnetPublishForReload: IOptProperty<Boolean> get() = _useDotnetPublishForReload
    val useDotnetBuildForReload: IOptProperty<Boolean> get() = _useDotnetBuildForReload
    val useUnloadableContext: IOptProperty<Boolean> get() = _useUnloadableContext
    val dontGuessTFM: IOptProperty<Boolean> get() = _dontGuessTFM
    val selectedCustomJit: IProperty<String?> get() = _selectedCustomJit
    //methods
    //initializer
    init {
        _isVisible.optimizeNested = true
        _isLoading.optimizeNested = true
        _unavailabilityReason.optimizeNested = true
        _errorCode.optimizeNested = true
        _currentContent.optimizeNested = true
        _sourceFilePath.optimizeNested = true
        _caretOffset.optimizeNested = true
        _documentModificationStamp.optimizeNested = true
        _snapshotContent.optimizeNested = true
        _hasSnapshot.optimizeNested = true
        _showAsmComments.optimizeNested = true
        _useTieredJit.optimizeNested = true
        _usePGO.optimizeNested = true
        _diffable.optimizeNested = true
        _runAppMode.optimizeNested = true
        _useNoRestoreFlag.optimizeNested = true
        _useDotnetPublishForReload.optimizeNested = true
        _useDotnetBuildForReload.optimizeNested = true
        _useUnloadableContext.optimizeNested = true
        _dontGuessTFM.optimizeNested = true
        _selectedCustomJit.optimizeNested = true
    }
    
    init {
        bindableChildren.add("show" to _show)
        bindableChildren.add("isVisible" to _isVisible)
        bindableChildren.add("isLoading" to _isLoading)
        bindableChildren.add("unavailabilityReason" to _unavailabilityReason)
        bindableChildren.add("errorCode" to _errorCode)
        bindableChildren.add("currentContent" to _currentContent)
        bindableChildren.add("sourceFilePath" to _sourceFilePath)
        bindableChildren.add("caretOffset" to _caretOffset)
        bindableChildren.add("documentModificationStamp" to _documentModificationStamp)
        bindableChildren.add("snapshotContent" to _snapshotContent)
        bindableChildren.add("hasSnapshot" to _hasSnapshot)
        bindableChildren.add("showAsmComments" to _showAsmComments)
        bindableChildren.add("useTieredJit" to _useTieredJit)
        bindableChildren.add("usePGO" to _usePGO)
        bindableChildren.add("diffable" to _diffable)
        bindableChildren.add("runAppMode" to _runAppMode)
        bindableChildren.add("useNoRestoreFlag" to _useNoRestoreFlag)
        bindableChildren.add("useDotnetPublishForReload" to _useDotnetPublishForReload)
        bindableChildren.add("useDotnetBuildForReload" to _useDotnetBuildForReload)
        bindableChildren.add("useUnloadableContext" to _useUnloadableContext)
        bindableChildren.add("dontGuessTFM" to _dontGuessTFM)
        bindableChildren.add("selectedCustomJit" to _selectedCustomJit)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdProperty<String?>(null, __StringNullableSerializer),
        RdProperty<String?>(null, __StringNullableSerializer),
        RdProperty<String?>(null, __StringNullableSerializer),
        RdProperty<String?>(null, __StringNullableSerializer),
        RdProperty<Int?>(null, __IntNullableSerializer),
        RdProperty<Long?>(null, __LongNullableSerializer),
        RdProperty<String?>(null, __StringNullableSerializer),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdProperty<String?>(null, __StringNullableSerializer)
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
            print("unavailabilityReason = "); _unavailabilityReason.print(printer); println()
            print("errorCode = "); _errorCode.print(printer); println()
            print("currentContent = "); _currentContent.print(printer); println()
            print("sourceFilePath = "); _sourceFilePath.print(printer); println()
            print("caretOffset = "); _caretOffset.print(printer); println()
            print("documentModificationStamp = "); _documentModificationStamp.print(printer); println()
            print("snapshotContent = "); _snapshotContent.print(printer); println()
            print("hasSnapshot = "); _hasSnapshot.print(printer); println()
            print("showAsmComments = "); _showAsmComments.print(printer); println()
            print("useTieredJit = "); _useTieredJit.print(printer); println()
            print("usePGO = "); _usePGO.print(printer); println()
            print("diffable = "); _diffable.print(printer); println()
            print("runAppMode = "); _runAppMode.print(printer); println()
            print("useNoRestoreFlag = "); _useNoRestoreFlag.print(printer); println()
            print("useDotnetPublishForReload = "); _useDotnetPublishForReload.print(printer); println()
            print("useDotnetBuildForReload = "); _useDotnetBuildForReload.print(printer); println()
            print("useUnloadableContext = "); _useUnloadableContext.print(printer); println()
            print("dontGuessTFM = "); _dontGuessTFM.print(printer); println()
            print("selectedCustomJit = "); _selectedCustomJit.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): AsmViewerModel   {
        return AsmViewerModel(
            _show.deepClonePolymorphic(),
            _isVisible.deepClonePolymorphic(),
            _isLoading.deepClonePolymorphic(),
            _unavailabilityReason.deepClonePolymorphic(),
            _errorCode.deepClonePolymorphic(),
            _currentContent.deepClonePolymorphic(),
            _sourceFilePath.deepClonePolymorphic(),
            _caretOffset.deepClonePolymorphic(),
            _documentModificationStamp.deepClonePolymorphic(),
            _snapshotContent.deepClonePolymorphic(),
            _hasSnapshot.deepClonePolymorphic(),
            _showAsmComments.deepClonePolymorphic(),
            _useTieredJit.deepClonePolymorphic(),
            _usePGO.deepClonePolymorphic(),
            _diffable.deepClonePolymorphic(),
            _runAppMode.deepClonePolymorphic(),
            _useNoRestoreFlag.deepClonePolymorphic(),
            _useDotnetPublishForReload.deepClonePolymorphic(),
            _useDotnetBuildForReload.deepClonePolymorphic(),
            _useUnloadableContext.deepClonePolymorphic(),
            _dontGuessTFM.deepClonePolymorphic(),
            _selectedCustomJit.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val Solution.asmViewerModel get() = getOrCreateExtension("asmViewerModel", ::AsmViewerModel)

