package flstudio

import delphiLocalEpochDaysToUnixUtcEpochSeconds
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.toKStringFromUtf16
import kotlinx.cinterop.toLong
import kotlinx.datetime.Clock
import okio.Path.Companion.toPath
import okio.use
import openProcess
import platform.windows.GetLastError
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.MAX_PATH
import platform.windows.PROCESS_VM_READ
import tlhelp32.CreateToolhelp32Snapshot
import tlhelp32.MODULEENTRY32W
import tlhelp32.Module32FirstW
import tlhelp32.Module32NextW
import tlhelp32.TH32CS_SNAPMODULE
import toBoolean
import use
import windows.Process
import windows.StringSize
import windows.address
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalForeignApi::class)
class FLEngine(
    private val flStudio: FLStudio,
    private val processId: UInt,
) {
    private val memoryAddressOffset: Long = CreateToolhelp32Snapshot(TH32CS_SNAPMODULE.toUInt(), processId)
        .takeIf { it != INVALID_HANDLE_VALUE }
        ?.use { snapshotHandle ->
            memScoped {
                val moduleEntry = alloc<MODULEENTRY32W> {
                    dwSize = sizeOf<MODULEENTRY32W>().toUInt()
                }
                if (Module32FirstW(snapshotHandle, moduleEntry.ptr).toBoolean()) do {
                    if (moduleEntry.szModule.toKStringFromUtf16().startsWith("FLEngine")) {
                        return@memScoped moduleEntry.modBaseAddr
                    }
                } while (Module32NextW(snapshotHandle, moduleEntry.ptr).toBoolean())
                null
            }?.toLong() ?: error("Unable to find engine")
        } ?: error("Unable to take snapshot: ${GetLastError()}")
    
    private var previousTimeSpent: Long = 0

    fun use(block: FLEngineProcess.() -> Unit): Result<Unit> {
        val handle = openProcess(processId, PROCESS_VM_READ)
        return if (handle != null) {
            Result.success(FLEngineProcess(handle, memoryAddressOffset).use(block))
        } else {
            Result.failure(RuntimeException("Unable to open process with id $processId"))
        }
    }

    @Suppress("unused")
    inner class FLEngineProcess internal constructor(handle: HANDLE, memoryAddressOffset: Long) :
        Process(handle, memoryAddressOffset) {

        val selectedPattern: Int by address(0xF5CCF8)
        val bpm: Int by address(0xF5FAF0)
        val isPlaying: Boolean by address(0x1080F0C, Int::toBoolean)
        val dateCreated: Long by address(0x1082938, Double::delphiLocalEpochDaysToUnixUtcEpochSeconds)
        private val timeSpentMarker: Long by address(0x1082940, Double::delphiLocalEpochDaysToUnixUtcEpochSeconds)
        private val flpPathAddress: Long by address(0x10829F0) { address: Long -> address - memoryAddressOffset }
        val flpPath: String by address(::flpPathAddress, MAX_PATH, StringSize.Utf16)
        val songPositionMillis: Long by address(0x10A6698)

        val flpName: String get() = flpPath.toPath().name

        private val isIdle: Boolean get() = !isPlaying && (flStudio.isMinimized || !flStudio.isFocused)

        val timeSpent: Long get() = if (isIdle) {
            previousTimeSpent
        } else {
            (Clock.System.now().epochSeconds - timeSpentMarker).also { previousTimeSpent = it }
        }

        val formattedTimeSpent: String get() {
            val duration = timeSpent.seconds
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
}
