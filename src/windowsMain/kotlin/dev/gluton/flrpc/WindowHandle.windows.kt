package dev.gluton.flrpc

import kotlinx.cinterop.CPointed
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toLong
import okio.use
import platform.windows.EnumWindows
import platform.windows.GetForegroundWindow
import platform.windows.GetWindowTextLengthW
import platform.windows.GetWindowTextW
import platform.windows.GetWindowThreadProcessId
import platform.windows.HWND
import platform.windows.IsIconic
import platform.windows.IsWindowVisible
import platform.windows.LPARAM
import platform.windows.TRUE

value class WindowsWindowHandle(val value: HWND): WindowHandle {
    override val title: String? get() = GetWindowTextLengthW(value).takeUnlessZero()?.inc()?.let { length ->
        useUtf16StringBuffer(length.toLong()) {
            GetWindowTextW(value, it, length).takeUnlessZero() ?: return null
        }
    }
    override val processId: UInt? get() = useUIntBuffer { uIntPointer ->
        GetWindowThreadProcessId(value, uIntPointer).takeUnlessZero() ?: return null
    }
    override val isVisible: Boolean get() = IsWindowVisible(value).toBoolean()
    override val isMinimized: Boolean get() = IsIconic(value).toBoolean()
    override val isFocused: Boolean get() = GetForegroundWindow()?.let(::WindowsWindowHandle)?.processId == processId
}

actual fun WindowHandle.Companion.fromExecutableName(executableName: String): WindowHandle? {
    val data = object {
        val executableName = executableName
        var windowHandle: WindowHandle? = null
    }
    enumWindows(data) { windowHandle ->
        if (!windowHandle.isVisible || windowHandle.title.isNullOrBlank()) return@enumWindows true

        val processHandle = openProcess(windowHandle.processId ?: 0u, ProcessFlags.ReadMemory or ProcessFlags.Info)
        val foundWindow = processHandle?.use { it.executablePath?.name == this@enumWindows.executableName } ?: false
        if (foundWindow) {
            this@enumWindows.windowHandle = windowHandle
        }
        !foundWindow
    }
    return data.windowHandle
}

/**
 * @param onEach return false to stop enumerating
 */
private fun <T> enumWindows(data: T, onEach: T.(WindowHandle) -> Boolean) {
    val stableDataRef = data?.let { StableRef.create(EnumWindowsData(data, onEach)) }
    val enumWindowsFunction = staticCFunction { windowHandle: HWND?, enumWindowsDataAddress: LPARAM ->
        val enumWindowsData = enumWindowsDataAddress.toCPointer<CPointed>()!!.asStableRef<EnumWindowsData<T>>().get()
        windowHandle?.let {
            val (data, onEachWindow) = enumWindowsData
            data.onEachWindow(WindowsWindowHandle(it)).toInt()
        } ?: TRUE
    }
    EnumWindows(enumWindowsFunction, stableDataRef?.asCPointer().toLong())
    stableDataRef?.dispose()
}

private data class EnumWindowsData<T>(
    val data: T,
    val onEachWindow: T.(WindowHandle) -> Boolean,
)