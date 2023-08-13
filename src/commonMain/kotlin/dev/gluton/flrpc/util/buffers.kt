package dev.gluton.flrpc.util

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.value

inline fun <reified T : CVariable> useBuffer(block: MemScope.(bufferPointer: CPointer<T>) -> Unit): T = memScoped {
    alloc<T>().apply { block(ptr) }
}

inline fun <reified T : CPointed> usePointerVarBuffer(block: MemScope.(bufferPointer: CPointer<CPointerVar<T>>) -> Unit): CPointer<T>? = memScoped {
    allocPointerTo<T>().apply { block(ptr) }.value
}

inline fun useByteBuffer(block: MemScope.(bufferPointer: CPointer<ByteVar>) -> Unit): Byte = memScoped {
    alloc<ByteVar>().apply { block(ptr) }.value
}

inline fun useShortBuffer(block: MemScope.(bufferPointer: CPointer<ShortVar>) -> Unit): Short = memScoped {
    alloc<ShortVar>().apply { block(ptr) }.value
}

inline fun useIntBuffer(block: MemScope.(bufferPointer: CPointer<IntVar>) -> Unit): Int = memScoped {
    alloc<IntVar>().apply { block(ptr) }.value
}

inline fun useUIntBuffer(block: MemScope.(bufferPointer: CPointer<UIntVar>) -> Unit): UInt = memScoped {
    alloc<UIntVar>().apply { block(ptr) }.value
}

inline fun useLongBuffer(block: MemScope.(bufferPointer: CPointer<LongVar>) -> Unit): Long = memScoped {
    alloc<LongVar>().apply { block(ptr) }.value
}

inline fun useFloatBuffer(block: MemScope.(bufferPointer: CPointer<FloatVar>) -> Unit): Float = memScoped {
    alloc<FloatVar>().apply { block(ptr) }.value
}

inline fun useDoubleBuffer(block: MemScope.(bufferPointer: CPointer<DoubleVar>) -> Unit): Double = memScoped {
    alloc<DoubleVar>().apply { block(ptr) }.value
}

inline fun useUtf8StringBuffer(size: Long, block: MemScope.(bufferPointer: CArrayPointer<ByteVar>) -> Unit): String = memScoped {
    allocArray<ByteVar>(size).also { block(it) }.toKString()
}

inline fun useUtf16StringBuffer(size: Long, block: MemScope.(bufferPointer: CArrayPointer<UShortVar>) -> Unit): String = memScoped {
    allocArray<UShortVar>(size).also { block(it) }.toKStringFromUtf16()
}