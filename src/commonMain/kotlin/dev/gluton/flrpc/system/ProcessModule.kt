package dev.gluton.flrpc.system

import kotlin.properties.ReadOnlyProperty

abstract class ProcessModule(processHandle: ProcessHandle, val addressOffset: Long = 0) :
    ProcessHandle by processHandle {

    companion object {
        inline fun <reified T : Number> ProcessModule.address(address: Long) = address<T, T>(address) { it }
        inline fun <reified T : Number> ProcessModule.address(crossinline addressProvider: () -> Long) =
            address<T, T>(addressProvider) { it }
        inline fun <reified T : Number, R> ProcessModule.address(address: Long, crossinline transform: (T) -> R) =
            address({ address }, transform)
        inline fun <reified T : Number, R> ProcessModule.address(
            crossinline addressProvider: () -> Long,
            crossinline transform: (T) -> R,
        ): ReadOnlyProperty<Any, R> {
            @Suppress("Unchecked_cast")
            val readNumber = when (T::class) {
                Byte::class -> ProcessHandle::readFloat
                Short::class -> ProcessHandle::readShort
                Int::class -> ProcessHandle::readInt
                Long::class -> ProcessHandle::readLong
                Float::class -> ProcessHandle::readFloat
                Double::class -> ProcessHandle::readDouble
                else -> error("Unsupported address type: ${T::class.qualifiedName}")
            } as ProcessHandle.(Long) -> T
            return ReadOnlyProperty { _, _ -> transform(readNumber(addressOffset + addressProvider())) }
        }

        inline fun ProcessModule.utf8StringAddress(address: Long, length: Long) = utf8StringAddress(address, length) { it }
        inline fun ProcessModule.utf8StringAddress(crossinline addressProvider: () -> Long, length: Long) =
            utf8StringAddress(addressProvider, length) { it }
        inline fun <R> ProcessModule.utf8StringAddress(address: Long, length: Long, crossinline transform: (String) -> R) =
            utf8StringAddress({ address }, length, transform)
        inline fun <R> ProcessModule.utf8StringAddress(
            crossinline addressProvider: () -> Long,
            length: Long,
            crossinline transform: (String) -> R,
        ) = ReadOnlyProperty<Any, R> { _, _ -> transform(readUtf8String(addressOffset + addressProvider(), length)) }

        inline fun ProcessModule.utf16StringAddress(address: Long, length: Long) = utf16StringAddress(address, length) { it }
        inline fun ProcessModule.utf16StringAddress(crossinline addressProvider: () -> Long, length: Long) =
            utf16StringAddress(addressProvider, length) { it }
        inline fun <R> ProcessModule.utf16StringAddress(address: Long, length: Long, crossinline transform: (String) -> R) =
            utf16StringAddress({ address }, length, transform)
        inline fun <R> ProcessModule.utf16StringAddress(
            crossinline addressProvider: () -> Long,
            length: Long,
            crossinline transform: (String) -> R,
        ) = ReadOnlyProperty<Any, R> { _, _ -> transform(readUtf16String(addressOffset + addressProvider(), length)) }
    }
}

