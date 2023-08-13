package dev.gluton.flrpc.system

import dev.gluton.flrpc.util.useByteBuffer
import dev.gluton.flrpc.util.useDoubleBuffer
import dev.gluton.flrpc.util.useFloatBuffer
import dev.gluton.flrpc.util.useIntBuffer
import dev.gluton.flrpc.util.useLongBuffer
import dev.gluton.flrpc.util.useShortBuffer
import dev.gluton.flrpc.util.useUtf16StringBuffer
import dev.gluton.flrpc.util.useUtf8StringBuffer
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.sizeOf
import okio.Closeable
import okio.Path

interface ProcessHandle : Closeable {
    val executablePath: Path?
    val isRunning: Boolean

    fun readMemory(address: Long, buffer: COpaquePointer, sizeBytes: ULong)

    companion object
}

fun ProcessHandle.readByte(address: Long): Byte = useByteBuffer {
    readMemory(address, it, sizeOf<ByteVar>().toULong())
}
fun ProcessHandle.readShort(address: Long): Short = useShortBuffer {
    readMemory(address, it, sizeOf<ShortVar>().toULong())
}
fun ProcessHandle.readInt(address: Long): Int = useIntBuffer {
    readMemory(address, it, sizeOf<IntVar>().toULong())
}
fun ProcessHandle.readLong(address: Long): Long = useLongBuffer {
    readMemory(address, it, sizeOf<LongVar>().toULong())
}
fun ProcessHandle.readFloat(address: Long): Float = useFloatBuffer {
    readMemory(address, it, sizeOf<FloatVar>().toULong())
}
fun ProcessHandle.readDouble(address: Long): Double = useDoubleBuffer {
    readMemory(address, it, sizeOf<DoubleVar>().toULong())
}
fun ProcessHandle.readUtf8String(address: Long, length: Long): String = useUtf8StringBuffer(length) {
    readMemory(address, it, (sizeOf<ByteVar>() * length).toULong())
}
fun ProcessHandle.readUtf16String(address: Long, length: Long): String = useUtf16StringBuffer(length) {
    readMemory(address, it, (sizeOf<UShortVar>() * length).toULong())
}

expect fun openProcess(processId: UInt, flags: ProcessFlags): ProcessHandle?

expect fun findModuleAddressByName(processId: UInt, name: String): Long?

value class ProcessFlags(internal val value: UInt) {
    infix fun or(other: ProcessFlags): ProcessFlags = ProcessFlags(value or other.value)

    companion object
}

expect val ProcessFlags.Companion.ReadMemory: ProcessFlags
expect val ProcessFlags.Companion.Info: ProcessFlags