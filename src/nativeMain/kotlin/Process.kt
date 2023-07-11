@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Closeable
import platform.windows.HANDLE
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KClass

interface Process : Closeable {
    val handle: COpaquePointer
    val addressOffset: Long get() = 0

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
    val readNumber = T::class.getReader()
    return ReadOnlyProperty { _, _ ->
        transformer(handle.readNumber(addressOffset + address))
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

inline fun Process.address(address: Long, maxLength: Int, size: StringSize = StringSize.Utf8) =
    address(address, maxLength, size) { it }
inline fun <R> Process.address(
    address: Long,
    maxLength: Int,
    size: StringSize = StringSize.Utf8,
    crossinline transformer: (String) -> R
) = address({ address }, maxLength, size, transformer)

inline fun Process.address(crossinline addressProvider: () -> Long, maxLength: Int, size: StringSize = StringSize.Utf8) =
    address(addressProvider, maxLength, size) { it }
inline fun <R> Process.address(
    crossinline addressProvider: () -> Long,
    maxLength: Int,
    size: StringSize = StringSize.Utf8,
    crossinline transformer: (String) -> R
): ReadOnlyProperty<Any, R> {
    val readString = size.getReader()
    return ReadOnlyProperty { _, _ ->
        transformer(handle.readString(addressOffset + addressProvider(), maxLength))
    }
}

inline fun StringSize.getReader(): HANDLE.(Long, Int) -> String = when (this) {
    StringSize.Utf8 -> HANDLE::readString
    StringSize.Utf16 -> HANDLE::readWString
}

enum class StringSize {
    Utf8,
    Utf16,
}
