package flstudio

import delphiLocalEpochDaysToUnixUtcEpochSeconds
import executablePath
import getSnapshotOfProcessModules
import isVisible
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toCPointer
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.toLong
import kotlinx.cinterop.useContents
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.Path.Companion.toPath
import okio.use
import openProcess
import platform.windows.DWORD
import platform.windows.EnumWindows
import platform.windows.GetExitCodeProcess
import platform.windows.GetForegroundWindow
import platform.windows.HANDLE
import platform.windows.HKEY_CURRENT_USER
import platform.windows.HWND
import platform.windows.HWNDVar
import platform.windows.IsIconic
import platform.windows.LPARAM
import platform.windows.MAX_PATH
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
import usePointerVarBuffer
import useUIntBuffer
import Process
import StringSize
import address
import platform.windows.HWND__
import use
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class FLStudio private constructor(private val windowHandle: HWND) {
    val processId: DWORD = windowHandle.processId?.takeUnlessZero()
        ?: error("Unable to get process id on initialization")

    val windowTitle: String get() = windowHandle.title ?: ""
    val name: String get() = windowTitle.substringAfterLast('-').trim().takeIf(String::isNotEmpty) ?: "FL Studio"

    val isFocused: Boolean get() = GetForegroundWindow()?.processId != processId
    val isMinimized: Boolean get() = IsIconic(windowHandle).toBoolean()
    val isRunning: Boolean get() = openProcess(processId, PROCESS_QUERY_INFORMATION)?.use { flHandle ->
        useUIntBuffer { GetExitCodeProcess(flHandle, it) } == STILL_ACTIVE
    } ?: false

    fun <T> useEngine(block: FLEngine.() -> T): Result<T> {
        val handle = openProcess(processId, PROCESS_VM_READ)
        return if (handle != null) {
            Result.success(FLEngineProcess(handle).use(block))
        } else {
            Result.failure(RuntimeException("Unable to open process with id $processId"))
        }
    }

    sealed interface FLEngine {
        val version: String
        val songPositionMillis: Long
        val selectedPattern: Int
        val bpm: Int
        val isPlaying: Boolean

        val flpPath: String
        val flpName: String

        val dateCreated: Instant
        val timeSpentMillis: Long
        val formattedTimeSpent: String
    }

    private var previousTimeSpent: Long = 0
    private val engineAddressOffset: Long = getSnapshotOfProcessModules(processId)
        .find { it.useContents { szModule.toKStringFromUtf16().startsWith("FLEngine") } }
        ?.useContents { modBaseAddr.toLong() } ?: error("Unable to find FLEngine's memory address")

    @Suppress("unused")
    private inner class FLEngineProcess(override val handle: HANDLE) : FLEngine, Process {
        override val addressOffset: Long by this@FLStudio::engineAddressOffset

        override val version: String by address(0x497F67C, 32, StringSize.Utf16)
        override val songPositionMillis: Long by address(0x10A6698)
        override val selectedPattern: Int by address(0xF5CCF8)
        override val bpm: Int by address(0xF5FAF0)
        override val isPlaying: Boolean by address(0x1080F0C, Int::toBoolean)

        override val flpPath: String by address(::flpPathAddress, MAX_PATH, StringSize.Utf16)
        private val flpPathAddress: Long by address(0x10829F0) { address: Long -> address - addressOffset }
        override val flpName: String get() = flpPath.toPath().name

        override val dateCreated: Instant by address(0x1082938) { days: Double ->
            Instant.fromEpochSeconds(days.delphiLocalEpochDaysToUnixUtcEpochSeconds())
        }
        override val timeSpentMillis: Long get() {
            if (!isIdle) {
                previousTimeSpent = (Clock.System.now().epochSeconds - timeSpentMarker)
            }
            return previousTimeSpent
        }
        private val timeSpentMarker: Long by address(0x1082940, Double::delphiLocalEpochDaysToUnixUtcEpochSeconds)
        private val isIdle: Boolean get() = !isPlaying && (isMinimized || !isFocused)
        override val formattedTimeSpent: String get() {
            val duration = timeSpentMillis.seconds
            val minutes = duration.inWholeMinutes
            val hours = duration.inWholeHours
            return when {
                minutes < 1 -> "less than a minute"
                minutes == 1L -> "1 minute"
                hours < 2 -> "$minutes minutes"
                else -> "$hours hours"
            }
        }
    } 

    companion object {
        private val flExecutablePath: String? by lazy {
            readRegistryString(HKEY_CURRENT_USER, "SOFTWARE\\Image-Line\\Shared\\Paths", "FL Studio")
        }

        fun attach(): FLStudio? = getFLWindowHandle()?.let(::FLStudio)
        private fun getFLWindowHandle(): HWND? {
            if (flExecutablePath == null) return null

            fun findFLWindowHandle(windowHandle: HWND?, hwndPointerAddress: LPARAM): WINBOOL {
                val isFl = checkIfFLWindow(windowHandle)
                if (isFl) {
                    hwndPointerAddress.toCPointer<HWNDVar>()?.set(windowHandle)
                }
                return isFl.not().toInt()
            }

            return usePointerVarBuffer<HWND__> { bufferPointer ->
                EnumWindows(staticCFunction(::findFLWindowHandle), bufferPointer.rawValue.toLong())
            }
        }
        private inline fun checkIfFLWindow(windowHandle: HWND?): Boolean {
            with(windowHandle) {
                if (this == null || !isVisible || title.isNullOrBlank()) return false
            }
            return openProcess(windowHandle, PROCESS_VM_READ or PROCESS_QUERY_INFORMATION)?.use {
                it.executablePath == flExecutablePath
            } ?: false
        }
    }
}