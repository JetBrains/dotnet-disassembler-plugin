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
 * #### Generated from [JitAsmProviderModel.kt:7]
 */
class JitAsmProviderModel private constructor(
    private val _getJitCodegenForSelectedElement: RdCall<Unit, String?>
) : RdExtBase() {
    //companion
    
    companion object : ISerializersOwner {
        
        override fun registerSerializersCore(serializers: ISerializers)  {
        }
        
        
        
        
        private val __StringNullableSerializer = FrameworkMarshallers.String.nullable()
        
        const val serializationHash = -7815335541907647124L
        
    }
    override val serializersOwner: ISerializersOwner get() = JitAsmProviderModel
    override val serializationHash: Long get() = JitAsmProviderModel.serializationHash
    
    //fields
    val getJitCodegenForSelectedElement: IRdCall<Unit, String?> get() = _getJitCodegenForSelectedElement
    //methods
    //initializer
    init {
        bindableChildren.add("getJitCodegenForSelectedElement" to _getJitCodegenForSelectedElement)
    }
    
    //secondary constructor
    internal constructor(
    ) : this(
        RdCall<Unit, String?>(FrameworkMarshallers.Void, __StringNullableSerializer)
    )
    
    //equals trait
    //hash code trait
    //pretty print
    override fun print(printer: PrettyPrinter)  {
        printer.println("JitAsmProviderModel (")
        printer.indent {
            print("getJitCodegenForSelectedElement = "); _getJitCodegenForSelectedElement.print(printer); println()
        }
        printer.print(")")
    }
    //deepClone
    override fun deepClone(): JitAsmProviderModel   {
        return JitAsmProviderModel(
            _getJitCodegenForSelectedElement.deepClonePolymorphic()
        )
    }
    //contexts
    //threading
    override val extThreading: ExtThreadingKind get() = ExtThreadingKind.Default
}
val Solution.jitAsmProviderModel get() = getOrCreateExtension("jitAsmProviderModel", ::JitAsmProviderModel)

