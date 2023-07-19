package dev.gluton.flrpc

import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.alloc
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.toLong
import kotlinx.cinterop.useContents
import kotlinx.cinterop.value
import okio.Path
import okio.Path.Companion.toPath
import platform.windows.CloseHandle
import platform.windows.DWORDVar
import platform.windows.FALSE
import platform.windows.GetExitCodeProcess
import platform.windows.HANDLE
import platform.windows.MAX_PATH
import platform.windows.OpenProcess
import platform.windows.PROCESS_QUERY_INFORMATION
import platform.windows.PROCESS_VM_READ
import platform.windows.QueryFullProcessImageNameW
import platform.windows.ReadProcessMemory
import platform.windows.STILL_ACTIVE

data class WindowsProcessHandle(
    val value: HANDLE,
    val flags: ProcessFlags,
) : ProcessHandle {
    private var isClosed: Boolean = false

    override val executablePath: Path?
        get() {
            check(!isClosed) { "closed" }
            return useUtf16StringBuffer(MAX_PATH.toLong()) {
                val fileNameLength = alloc<DWORDVar>().apply { value = MAX_PATH.toUInt() }
                QueryFullProcessImageNameW(value, 0u, it, fileNameLength.ptr).takeUnlessZero() ?: return null
                fileNameLength.value.takeUnlessZero() ?: return null
            }.toPath()
        }
    override val isRunning: Boolean
        get() {
            check(!isClosed) { "closed" }
            return useUIntBuffer { GetExitCodeProcess(value, it) } == STILL_ACTIVE
        }

    override fun readMemory(address: Long, buffer: COpaquePointer, sizeBytes: ULong) {
        check(!isClosed) { "Unable to retrieve executablePath: ${this::class.simpleName} has been closed" }
        if (!ReadProcessMemory(value, address.toCPointer(), buffer, sizeBytes, null).toBoolean()) {
            throw WindowsException("Failed reading process memory")
        }
    }

    override fun close() {
        if (isClosed) return
        isClosed = CloseHandle(value).toBoolean()
        if (!isClosed) {
            throw WindowsException("Unable to close handle")
        }
    }
}

actual fun openProcess(processId: UInt, flags: ProcessFlags): ProcessHandle? =
    OpenProcess(flags.value, FALSE, processId)?.let { WindowsProcessHandle(it, flags) }

actual fun findModuleAddressByName(processId: UInt, name: String): Long? =
    ModuleSnapshot.create(processId)
        ?.find { it.useContents { szModule.toKStringFromUtf16() == name } }
        ?.useContents { modBaseAddr.toLong() }

private object WindowsProcessFlags {
    val ReadMemory = ProcessFlags(PROCESS_VM_READ.toUInt())
    val Info = ProcessFlags(PROCESS_QUERY_INFORMATION.toUInt())
}

actual val ProcessFlags.Companion.ReadMemory: ProcessFlags get() = WindowsProcessFlags.ReadMemory
actual val ProcessFlags.Companion.Info: ProcessFlags get() = WindowsProcessFlags.Info