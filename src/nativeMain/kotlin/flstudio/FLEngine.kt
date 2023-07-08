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
import readDouble
import readInt
import readLong
import readWString
import tlhelp32.CreateToolhelp32Snapshot
import tlhelp32.MODULEENTRY32W
import tlhelp32.Module32FirstW
import tlhelp32.Module32NextW
import tlhelp32.TH32CS_SNAPMODULE
import toBoolean
import use
import windows.WindowsProcess
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

    // TODO: create MemoryAddress delegate for a HandleProvider class
    private val selectedPatternAddress = 0xF5CCF8.engineOffset
    private val bpmAddress = 0xF5FAF0.engineOffset
    private val isPlayingAddress = 0x1080F0C.engineOffset
    private val dateCreateAddress = 0x1082938.engineOffset
    private val timeSpentMarkerAddress = 0x1082940.engineOffset
    private val flpPathPointerAddress = 0x10829F0.engineOffset
    private val songPositionMillisecondsAddress = 0x10A6698.engineOffset

    private val Int.engineOffset: Long get() = memoryAddressOffset + this
    
    private var previousTimeSpent: Long = 0

    fun use(block: FLEngineProcess.() -> Unit): Result<Unit> =
        openProcess(processId, PROCESS_VM_READ)?.let { handle -> Result.success(FLEngineProcess(handle).use(block)) }
            ?: Result.failure(RuntimeException("Unable to open process with id $processId"))

    @Suppress("unused")
    inner class FLEngineProcess internal constructor(handle: HANDLE) : WindowsProcess(handle) {

        val selectedPattern: Int get() = handle.readInt(selectedPatternAddress)
        val bpm: Int get() = handle.readInt(bpmAddress)
        val isPlaying: Boolean get() = handle.readInt(isPlayingAddress).toBoolean()
        val dateCreated: Long get() = delphiLocalEpochDaysToUnixUtcEpochSeconds(handle.readDouble(dateCreateAddress))
        private val timeSpentMarker: Long get() = delphiLocalEpochDaysToUnixUtcEpochSeconds(handle.readDouble(timeSpentMarkerAddress))
        val flpPath: String get() = handle.readWString(handle.readLong(flpPathPointerAddress), MAX_PATH)
        val flpName: String get() = flpPath.toPath().name
        val songPositionMillis: Long get() = handle.readLong(songPositionMillisecondsAddress)

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
