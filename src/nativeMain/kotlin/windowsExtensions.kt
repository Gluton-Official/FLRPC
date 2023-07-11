@file:OptIn(ExperimentalForeignApi::class)

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.CVariable
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.value
import platform.windows.CloseHandle
import platform.windows.DWORD
import platform.windows.DWORDVar
import platform.windows.ERROR_SUCCESS
import platform.windows.FALSE
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageW
import platform.windows.GetLastError
import platform.windows.GetWindowTextLengthW
import platform.windows.GetWindowTextW
import platform.windows.GetWindowThreadProcessId
import platform.windows.HANDLE
import platform.windows.HKEY
import platform.windows.HWND
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.IsWindowVisible
import platform.windows.LANG_NEUTRAL
import platform.windows.MAX_PATH
import platform.windows.OpenProcess
import platform.windows.QueryFullProcessImageNameW
import platform.windows.RRF_RT_REG_SZ
import platform.windows.ReadProcessMemory
import platform.windows.RegGetValueW
import platform.windows.SUBLANG_DEFAULT
import platform.windows.TRUE
import tlhelp32.CreateToolhelp32Snapshot
import tlhelp32.MODULEENTRY32W
import tlhelp32.Module32FirstW
import tlhelp32.Module32NextW
import tlhelp32.TH32CS_SNAPMODULE

inline fun <reified T : CVariable> HANDLE.read(address: Long): T = useBuffer {
    ReadProcessMemory(this@read, address.toCPointer(), it, sizeOf<T>().toULong(), null)
}
inline fun HANDLE.readInt(address: Long): Int = useIntBuffer {
    ReadProcessMemory(this@readInt, address.toCPointer(), it, sizeOf<IntVar>().toULong(), null)
}
inline fun HANDLE.readLong(address: Long): Long = useLongBuffer {
    ReadProcessMemory(this@readLong, address.toCPointer(), it, sizeOf<LongVar>().toULong(), null)
}
inline fun HANDLE.readDouble(address: Long): Double = useDoubleBuffer {
    ReadProcessMemory(this@readDouble, address.toCPointer(), it, sizeOf<DoubleVar>().toULong(), null)
}
inline fun HANDLE.readString(address: Long, length: Int): String = useUtf8StringBuffer(length) {
    ReadProcessMemory(this@readString, address.toCPointer(), it, (sizeOf<ByteVar>() * length).toULong(), null)
}
inline fun HANDLE.readWString(address: Long, length: Int): String = useUtf16StringBuffer(length) {
    ReadProcessMemory(this@readWString, address.toCPointer(), it, (sizeOf<UShortVar>() * length).toULong(), null)
}

/** `false` if 0, `true` otherwise */
inline fun Int.toBoolean(): Boolean = this != FALSE
inline fun Boolean.toInt(): Int = if (this) TRUE else FALSE

inline val HWND.title: String?
    get() {
        val titleLength = GetWindowTextLengthW(this).takeUnlessZero()?.inc() ?: return null
        return useUtf16StringBuffer(titleLength) { stringPointer ->
            GetWindowTextW(this@title, stringPointer, titleLength).takeUnlessZero() ?: return null
        }
    }
inline val HWND.processId: DWORD?
    get() = useUIntBuffer { uIntPointer ->
        GetWindowThreadProcessId(this@processId, uIntPointer).takeUnlessZero() ?: return null
    }
inline val HWND.isVisible: Boolean
    get() = IsWindowVisible(this).toBoolean()

inline fun openProcess(processId: UInt, accessFlags: Int, inheritHandle: Boolean = false): HANDLE? =
    OpenProcess(accessFlags.toUInt(), inheritHandle.toInt(), processId)
inline fun openProcess(windowHandle: HWND?, accessFlags: Int, inheritHandle: Boolean = false): HANDLE? {
    val processId = windowHandle?.processId ?: return null
    return openProcess(processId, accessFlags, inheritHandle)
}

//fun createProcessSnapshot(processId: UInt, flags: Int): HANDLE? =
//    CreateToolhelp32Snapshot(flags.toUInt(), processId).takeIf { it != INVALID_HANDLE_VALUE }
fun getSnapshotOfProcessModules(processId: UInt): List<CValue<MODULEENTRY32W>> {
    val snapshotHandle = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE.toUInt(), processId)
        .takeIf { it != INVALID_HANDLE_VALUE } ?: throwLastWindowsError("Unable to take snapshot")
    return snapshotHandle.use {
        buildList {
            memScoped {
                val moduleEntry = alloc<MODULEENTRY32W> {
                    dwSize = sizeOf<MODULEENTRY32W>().toUInt()
                }
                if (Module32FirstW(snapshotHandle, moduleEntry.ptr).toBoolean()) do {
                    add(moduleEntry.readValue())
                } while (Module32NextW(snapshotHandle, moduleEntry.ptr).toBoolean())
            }
        }
    }
}

@Throws(RuntimeException::class)
inline fun <T> HANDLE.use(block: (HANDLE) -> T) = block(this).also { if (!close()) throwLastWindowsError("Unable to close") }
inline fun HANDLE.close(): Boolean = CloseHandle(this).toBoolean()

inline val HANDLE.executablePath: String?
    get() = useUtf16StringBuffer(MAX_PATH) {
        val fileNameLength = alloc<DWORDVar>().apply { value = MAX_PATH.toUInt() }
        QueryFullProcessImageNameW(this@executablePath, 0u, it, fileNameLength.ptr).takeUnlessZero() ?: return null
        fileNameLength.value.takeUnlessZero() ?: return null
    }

inline fun readRegistryString(
    hKey: HKEY?,
    subKey: String? = null,
    name: String? = null,
): String? = useUtf16StringBuffer(MAX_PATH) { stringPtr ->
    val valueLength = alloc<DWORDVar>().apply { value = MAX_PATH.toUInt() }
    RegGetValueW(hKey, subKey, name, RRF_RT_REG_SZ.toUInt(), null, stringPtr, valueLength.ptr)
        .takeIf { it == ERROR_SUCCESS } ?: return null
}

@Suppress("FunctionName")
inline fun MAKELANGID(primaryLang: Int, secondaryLang: Int): DWORD = ((primaryLang shl 10) or secondaryLang).toUInt()

@Throws(RuntimeException::class)
fun throwLastWindowsError(message: String? = null): Nothing {
    val (errorCode, errorName) = getLastWindowsError()
    throw RuntimeException("$errorCode: $errorName${message?.let { ": $it" } ?: ""}")
}

fun getLastWindowsError(): Pair<DWORD, String> {
    val errorCode = GetLastError()
    return errorCode to useUtf16StringBuffer(256) {
        FormatMessageW(
            (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            null,
            errorCode,
            MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
            it,
            256u,
            null
        )
    }
}