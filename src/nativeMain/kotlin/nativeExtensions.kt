@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CArrayPointerVar
import kotlinx.cinterop.CPointed
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.CPointerVarOf
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.MemScope
import kotlinx.cinterop.NativeFreeablePlacement
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cstr
import kotlinx.cinterop.get
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.interpretPointed
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.cinterop.wcstr
import kotlin.experimental.ExperimentalTypeInference

typealias CString = CArrayPointer<ByteVar>
typealias CStringVar = CArrayPointerVar<ByteVar>
typealias WCString = CArrayPointer<UShortVar>
typealias WCStringVar = CArrayPointerVar<UShortVar>

inline fun <reified T : CVariable> new(init: T.() -> Unit = {}) = nativeHeap.alloc<T>(init)
inline fun CPointed.delete() = nativeHeap.free(rawPtr)

inline fun <reified T : CPointed> Long.toCPointerVar(): CPointerVar<T> = interpretPointed(nativeNullPtr + this)
inline fun Long.writeToAddressAsUInt(value: UInt): Boolean =
    toCPointer<UIntVar>()?.apply { pointed.value = value } != null

inline operator fun <reified T : CVariable, reified P : CPointer<T>?> P.inc(): P =
    if (this != null) interpretCPointer<T>(rawValue + sizeOf<T>())!! as P else this
inline operator fun <reified T : CVariable, reified P : CPointer<T>?> P.plus(offset: Long): P =
    if (this != null) interpretCPointer<T>(rawValue + sizeOf<T>() * offset)!! as P else this

class NullableCPointerIterator<T : CPointer<*>>(
    private val pointer: CArrayPointer<CPointerVarOf<T>>,
    private val length: Int,
) : Iterator<T?> {
    private var index = -1
    override fun hasNext() = index < length
    override fun next() = pointer[++index]
}
open class CPointerIterator<T : CPointer<*>>(
    private val pointer: CArrayPointer<CPointerVarOf<T>>,
    private val length: Int = Int.MAX_VALUE,
) : Iterator<T> {
    private var index = -1
    override fun hasNext() = pointer[index + 1] != null && index < length
    override fun next() = pointer[++index] ?: throw NoSuchElementException()
}

fun <T : CPointer<*>> CArrayPointer<CPointerVarOf<T>>.nullableIterator(length: Int) = NullableCPointerIterator(this, length)
fun <T : CPointer<*>> CArrayPointer<CPointerVarOf<T>>.iterator(length: Int = Int.MAX_VALUE) = CPointerIterator(this, length)

fun <T : CPointer<*>> CArrayPointer<CPointerVarOf<T>>.asNullableSequence(length: Int): Sequence<T?> = Sequence { nullableIterator(length) }
fun <T : CPointer<*>> CArrayPointer<CPointerVarOf<T>>.asSequence(length: Int = Int.MAX_VALUE): Sequence<T> = Sequence { iterator(length) }

fun <T : CPointer<*>, R> CArrayPointer<CPointerVarOf<T>>.mapUntilNullAndFree(
    length: Int = Int.MAX_VALUE,
    memoryScope: NativeFreeablePlacement = nativeHeap,
    transform: (T) -> R,
): List<R> = asSequence(length).map { item ->
    transform(item).also {
        memoryScope.free(item.rawValue)
    }
}.toList()
fun <T : CPointer<*>, R> CArrayPointer<CPointerVarOf<T>>.mapAndFree(
    length: Int,
    memoryScope: NativeFreeablePlacement = nativeHeap,
    transform: (T?) -> R
): List<R> = asNullableSequence(length).map { item ->
    transform(item).also {
        memoryScope.free(item?.rawValue ?: nativeNullPtr)
    }
}.toList()

inline fun <T : CPointer<*>> CPointer<CPointerVarOf<T>>.set(value: T?) {
    this.pointed.value = value
}

inline fun CArrayPointer<ByteVar>.write(value: String): CArrayPointer<ByteVar> = value.cstr.place(this)
inline fun CArrayPointer<UShortVar>.write(value: String): CArrayPointer<UShortVar> = value.wcstr.place(this)

inline fun <reified T : CVariable> useBuffer(
    block: MemScope.(bufferPointer: CPointer<T>) -> Unit
): T = memScoped {
    alloc<T>().apply { block(ptr) }
}
inline fun <reified T : CPointed> usePointerVarBuffer(
    block: MemScope.(bufferPointer: CPointer<CPointerVar<T>>) -> Unit
): CPointer<T>? = memScoped {
    allocPointerTo<T>().apply { block(ptr) }.value
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
inline fun useDoubleBuffer(block: MemScope.(bufferPointer: CPointer<DoubleVar>) -> Unit): Double = memScoped {
    alloc<DoubleVar>().apply { block(ptr) }.value
}
@OptIn(ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun <reified T : CVariable> useArrayBuffer(size: Int, block: MemScope.(bufferPointer: CArrayPointer<T>) -> Int): Array<T> = memScoped {
    val array = allocArray<T>(size)
    val resultingSize = block(array)
    require(resultingSize <= size)
    Array(resultingSize, array::get)
}
inline fun <reified T : CVariable> useArrayBuffer(size: Int, block: MemScope.(bufferPointer: CArrayPointer<T>) -> Unit): Array<T> = memScoped {
    allocArray<T>(size).also { block(it) }.run { Array(size, ::get) }
}
inline fun useUtf16StringBuffer(size: Int, block: MemScope.(bufferPointer: CArrayPointer<UShortVar>) -> Unit): String = memScoped {
    allocArray<UShortVar>(size).also { block(it) }.toKString()
}
/**
 * Provides a pointer to an allocated [UInt] array for use then converts it to a [UIntArray] with length returned from [block]
 *
 * @param block should return the intended size of the [UIntArray], must be less than or equal to [size]
 */
@OptIn(ExperimentalUnsignedTypes::class, ExperimentalTypeInference::class)
@OverloadResolutionByLambdaReturnType
inline fun useUIntArrayBuffer(size: Int, block: MemScope.(bufferPointer: CArrayPointer<UIntVar>) -> Int): UIntArray = memScoped {
    val array = allocArray<UIntVar>(size)
    val resultingSize = block(array)
    require(resultingSize <= size)
    UIntArray(resultingSize, array::get)
}
@OptIn(ExperimentalUnsignedTypes::class)
inline fun useUIntArrayBuffer(size: Int, block: MemScope.(bufferPointer: CArrayPointer<UIntVar>) -> Unit): UIntArray = memScoped {
    allocArray<UIntVar>(size).also { block(it) }.run { UIntArray(size, ::get) }
}