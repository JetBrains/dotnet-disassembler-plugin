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
 * #### Generated from [JitAsmViewerToolWindow.kt:12]
 */
class JitAsmViewerToolWindow private constructor(
    private val _toolWindowContent: RdOptionalProperty<com.jetbrains.ide.model.uiautomation.BeControl>,
    private val _activateToolWindow: RdSignal<Boolean>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
            val classLoader = javaClass.classLoader
            serializers.register(LazyCompanionMarshaller(RdId(1328437441103866286), classLoader, "com.jetbrains.rd.ide.model.BeJitAsmViewerToolWindowPanel"))
        }
        
        
        @JvmStatic
        @JvmName("internalCreateModel")
        @Deprecated("Use create instead", ReplaceWith("create(lifetime, protocol)"))
        internal fun createModel(lifetime: Lifetime, protocol: IProtocol): JitAsmViewerToolWindow  {
            @Suppress("DEPRECATION")
            return create(lifetime, protocol)
        }
        
        @JvmStatic
        @Deprecated("Use protocol.jitAsmViewerToolWindow or revise the extension scope instead", ReplaceWith("protocol.jitAsmViewerToolWindow"))
        fun create(lifetime: Lifetime, protocol: IProtocol): JitAsmViewerToolWindow  {
            IdeRoot.register(protocol.serializers)
            
            return JitAsmViewerToolWindow()
        }
        
        
        const val serializationHash = 4211825857250621732L
        
    }
    override val serializersOwner: ISerializersOwner get() = JitAsmViewerToolWindow
    override val serializationHash: Long get() = JitAsmViewerToolWindow.serializationHash
    
    //fields
    val toolWindowContent: IOptProperty<com.jetbrains.ide.model.uiautomation.BeControl> get() = _toolWindowContent
    val activateToolWindow: ISignal<Boolean> get() = _activateToolWindow
    //methods
    //initializer
    init {
        bindableChildren.add("toolWindowContent" to _toolWindowContent)
        bindableChildren.add("activateToolWindow" to _activateToolWindow)
    }
    
    //secondary constructor
    private constructor(
    ) : this(
        RdOptionalProperty<com.jetbrains.ide.model.uiautomation.BeControl>(AbstractPolymorphic(com.jetbrains.ide.model.uiautomation.BeControl)),
        RdSignal<Boolean>(FrameworkMarshallers.Bool)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("JitAsmViewerToolWindow (")
        printer.indent {
            print("toolWindowContent = "); _toolWindowContent.print(printer); println()
            print("activateToolWindow = "); _activateToolWindow.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): JitAsmViewerToolWindow   {
        return JitAsmViewerToolWindow(
            _toolWindowContent.deepClonePolymorphic(),
            _activateToolWindow.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val IProtocol.jitAsmViewerToolWindow get() = getOrCreateExtension(JitAsmViewerToolWindow::class) { @Suppress("DEPRECATION") JitAsmViewerToolWindow.create(lifetime, this) }



/**
 * #### Generated from [JitAsmViewerToolWindow.kt:18]
 */
class BeJitAsmViewerToolWindowPanel private constructor(
    private val _contentToShow: RdProperty<String?>,
    _enabled: RdProperty<Boolean>,
    _controlId: RdProperty<@org.jetbrains.annotations.NonNls String>,
    _uniqueId: RdOptionalProperty<Int>,
    _dataId: RdOptionalProperty<Int>,
    _focusable: RdOptionalProperty<Boolean>,
    _tooltip: RdProperty<@org.jetbrains.annotations.Nls String?>,
    _focus: RdSignal<Unit>,
    _visible: RdOptionalProperty<com.jetbrains.ide.model.uiautomation.ControlVisibility>
) : com.jetbrains.ide.model.uiautomation.BeControl (
    _enabled,
    _controlId,
    _uniqueId,
    _dataId,
    _focusable,
    _tooltip,
    _focus,
    _visible
) {
    //companion
    
    companion object : IMarshaller<BeJitAsmViewerToolWindowPanel> {
        override val _type: KClass<BeJitAsmViewerToolWindowPanel> = BeJitAsmViewerToolWindowPanel::class
        override val id: RdId get() = RdId(1328437441103866286)
        
        @Suppress("UNCHECKED_CAST")
        override fun read(ctx: SerializationCtx, buffer: AbstractBuffer): BeJitAsmViewerToolWindowPanel  {
            val _id = RdId.read(buffer)
            val _enabled = RdProperty.read(ctx, buffer, FrameworkMarshallers.Bool)
            val _controlId = RdProperty.read(ctx, buffer, __StringSerializer)
            val _uniqueId = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.Int)
            val _dataId = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.Int)
            val _focusable = RdOptionalProperty.read(ctx, buffer, FrameworkMarshallers.Bool)
            val _tooltip = RdProperty.read(ctx, buffer, __StringNullableSerializer)
            val _focus = RdSignal.read(ctx, buffer, FrameworkMarshallers.Void)
            val _visible = RdOptionalProperty.read(ctx, buffer, com.jetbrains.ide.model.uiautomation.ControlVisibility.marshaller)
            val _contentToShow = RdProperty.read(ctx, buffer, __StringNullableSerializer)
            return BeJitAsmViewerToolWindowPanel(_contentToShow, _enabled, _controlId, _uniqueId, _dataId, _focusable, _tooltip, _focus, _visible).withId(_id)
        }
        
        override fun write(ctx: SerializationCtx, buffer: AbstractBuffer, value: BeJitAsmViewerToolWindowPanel)  {
            value.rdid.write(buffer)
            RdProperty.write(ctx, buffer, value._enabled)
            RdProperty.write(ctx, buffer, value._controlId)
            RdOptionalProperty.write(ctx, buffer, value._uniqueId)
            RdOptionalProperty.write(ctx, buffer, value._dataId)
            RdOptionalProperty.write(ctx, buffer, value._focusable)
            RdProperty.write(ctx, buffer, value._tooltip)
            RdSignal.write(ctx, buffer, value._focus)
            RdOptionalProperty.write(ctx, buffer, value._visible)
            RdProperty.write(ctx, buffer, value._contentToShow)
        }
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        private val __StringSerializer = FrameworkMarshallers.String
        
    }
    //fields
    val contentToShow: IProperty<String?> get() = _contentToShow
    //methods
    //initializer
    init {
        _contentToShow.optimizeNested = true
    }
    
    init {
        bindableChildren.add("contentToShow" to _contentToShow)
    }
    
    //secondary constructor
    constructor(
    ) : this(
        RdProperty<String?>(null, __StringNullableSerializer),
        RdProperty<Boolean>(true, FrameworkMarshallers.Bool),
        RdProperty<@org.jetbrains.annotations.NonNls String>("", __StringSerializer),
        RdOptionalProperty<Int>(FrameworkMarshallers.Int),
        RdOptionalProperty<Int>(FrameworkMarshallers.Int),
        RdOptionalProperty<Boolean>(FrameworkMarshallers.Bool),
        RdProperty<@org.jetbrains.annotations.Nls String?>(null, __StringNullableSerializer),
        RdSignal<Unit>(FrameworkMarshallers.Void),
        RdOptionalProperty<com.jetbrains.ide.model.uiautomation.ControlVisibility>(com.jetbrains.ide.model.uiautomation.ControlVisibility.marshaller)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("BeJitAsmViewerToolWindowPanel (")
        printer.indent {
            print("contentToShow = "); _contentToShow.print(printer); println()
            print("enabled = "); _enabled.print(printer); println()
            print("controlId = "); _controlId.print(printer); println()
            print("uniqueId = "); _uniqueId.print(printer); println()
            print("dataId = "); _dataId.print(printer); println()
            print("focusable = "); _focusable.print(printer); println()
            print("tooltip = "); _tooltip.print(printer); println()
            print("focus = "); _focus.print(printer); println()
            print("visible = "); _visible.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): BeJitAsmViewerToolWindowPanel   {
        return BeJitAsmViewerToolWindowPanel(
            _contentToShow.deepClonePolymorphic(),
            _enabled.deepClonePolymorphic(),
            _controlId.deepClonePolymorphic(),
            _uniqueId.deepClonePolymorphic(),
            _dataId.deepClonePolymorphic(),
            _focusable.deepClonePolymorphic(),
            _tooltip.deepClonePolymorphic(),
            _focus.deepClonePolymorphic(),
            _visible.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
}
