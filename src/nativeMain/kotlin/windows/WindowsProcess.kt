@file:OptIn(ExperimentalForeignApi::class)

package windows

import close
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Closeable
import platform.windows.HANDLE
import readDouble
import readInt
import readLong
import readString
import readWString
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

open class Process(
    val handle: COpaquePointer,
    val memoryAddressOffset: Long = 0,
) : Closeable {
    override fun close() {
        if (!handle.close()) {
            throw RuntimeException("Unable to close handle for ${this::class.qualifiedName}")
        }
    }
}

inline fun <reified T : Number> Process.address(address: Long) = address<T, T>(address) { it }
inline fun <reified T : Number, R> Process.address(
    address: Long,
    crossinline transformer: (T) -> R,
): ReadOnlyProperty<Any, R> {
    val read = T::class.getReader()
    return ReadOnlyProperty { _, _ ->
        transformer(handle.read(memoryAddressOffset + address))
    }
}

inline fun Process.address(address: Long, length: Int, size: StringSize = StringSize.Utf8) =
    address(address, length, size) { it }
inline fun <R> Process.address(
    address: Long,
    length: Int,
    size: StringSize = StringSize.Utf8,
    crossinline transformer: (String) -> R
) = address({ address }, length, size, transformer)

inline fun Process.address(crossinline addressProvider: () -> Long, length: Int, size: StringSize = StringSize.Utf8) =
    address(addressProvider, length, size) { it }
inline fun <R> Process.address(
    crossinline addressProvider: () -> Long,
    length: Int,
    size: StringSize = StringSize.Utf8,
    crossinline transformer: (String) -> R
): ReadOnlyProperty<Any, R> {
    val read = size.getReader()
    return ReadOnlyProperty { _, _ ->
        transformer(handle.read(memoryAddressOffset + addressProvider(), length))
    }
}

// TODO: add remaining types i guess
@Suppress("Unchecked_cast")
inline fun <T : Number> KClass<T>.getReader(): HANDLE.(Long) -> T = when (this) {
    Int::class -> HANDLE::readInt
    Long::class -> HANDLE::readLong
    Double::class -> HANDLE::readDouble
    else -> error("Unsupported address type: $qualifiedName")
} as HANDLE.(Long) -> T

inline fun StringSize.getReader(): HANDLE.(Long, Int) -> String = when (this) {
    StringSize.Utf8 -> HANDLE::readString
    StringSize.Utf16 -> HANDLE::readWString
}

enum class StringSize {
    Utf8,
    Utf16,
}
