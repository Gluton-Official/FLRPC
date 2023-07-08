package flstudio

import close
import executablePath
import isVisible
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import openProcess
import platform.windows.DWORD
import platform.windows.EnumWindows
import platform.windows.GetExitCodeProcess
import platform.windows.GetForegroundWindow
import platform.windows.HKEY_CURRENT_USER
import platform.windows.HWND
import platform.windows.HWNDVar
import platform.windows.IsIconic
import platform.windows.LPARAM
import platform.windows.PROCESS_QUERY_INFORMATION
import platform.windows.PROCESS_VM_READ
import platform.windows.STILL_ACTIVE
import platform.windows.WINBOOL
import processId
import readRegistryString
import set
import takeUnlessZero
import title
import toBoolean
import toInt
import useAsProcess
import usePointerVarBuffer
import useUIntBuffer

@OptIn(ExperimentalForeignApi::class)
class FLStudio private constructor(private val windowHandle: HWND) {

    val processId: DWORD = windowHandle.processId?.takeUnlessZero() ?: error("Unable to get process id on initialization")
    val engine = FLEngine(this, processId)

    val windowTitle: String? get() = windowHandle.title
    val name: String? get() = windowTitle?.substringAfterLast('-')?.trim()
    val isFocused: Boolean get() = GetForegroundWindow()?.processId != processId
    val isMinimized: Boolean get() = IsIconic(windowHandle).toBoolean()
    val isRunning: Boolean get() = processId.useAsProcess(PROCESS_QUERY_INFORMATION.toUInt()) {
        useUIntBuffer { GetExitCodeProcess(this@useAsProcess, it) } == STILL_ACTIVE
    }.getOrDefault(false)

    companion object {
        fun attach(): FLStudio? = getFLWindowHandle()?.let(::FLStudio)

        private val flExecutablePath: String? by lazy {
            readRegistryString(HKEY_CURRENT_USER, "SOFTWARE\\Image-Line\\Shared\\Paths", "FL Studio")
        }

        private fun getFLWindowHandle(): HWND? {
            if (flExecutablePath == null) return null

            fun findFLWindowHandle(windowHandle: HWND?, hwndPointerAddress: LPARAM): WINBOOL {
                val isFl = checkIfFLWindow(windowHandle)
                if (isFl) {
                    hwndPointerAddress.toCPointer<HWNDVar>()?.set(windowHandle)
                }
                return isFl.not().toInt()
            }

            return usePointerVarBuffer { bufferPointer ->
                EnumWindows(staticCFunction(::findFLWindowHandle), bufferPointer.rawValue.toLong())
            }
        }

        private fun checkIfFLWindow(windowHandle: HWND?): Boolean {
            if (windowHandle == null ||
                !windowHandle.isVisible ||
                windowHandle.title?.isNotBlank() != true
            ) return false

            val handle = windowHandle.openProcess((PROCESS_VM_READ or PROCESS_QUERY_INFORMATION).toUInt())
            val isFLStudio = handle?.executablePath == flExecutablePath
            handle?.close()

            return isFLStudio
        }
    }
}