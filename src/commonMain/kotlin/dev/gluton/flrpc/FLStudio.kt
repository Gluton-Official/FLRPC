package dev.gluton.flrpc

import dev.gluton.flrpc.delphi.DelphiInstant
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.use
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

class FLStudio private constructor(windowHandle: WindowHandle) : Application(windowHandle) {
    val name: String = windowTitle?.substringAfterLast('-')?.trim()?.takeIf(String::isNotEmpty) ?: "FL Studio"

    fun <T> useEngine(block: FLEngine.() -> T): Result<T> =
        when (val processHandle = openProcess(processId, ProcessFlags.ReadMemory)) {
            null -> Result.failure(RuntimeException("Unable to open FLEngine with process id: $processId"))
            else -> Result.success(FLEngineImpl(processHandle).use(block))
        }

    sealed interface FLEngine {
        val version: String
        val songPosition: Duration
        val selectedPattern: Int
        val bpm: Int
        val isPlaying: Boolean

        val flpPath: String
        val flpName: String

        val dateCreated: Instant
        val timeSpent: Duration
    }

    private var previousTimeSpent: Duration = Duration.ZERO
    private val engineAddressOffset: Long = findModuleAddressByName(processId, "FLEngine_x64.dll")
        ?: error("Unable to find FLEngine's memory address")

    @Suppress("unused")
    private inner class FLEngineImpl(processHandle: ProcessHandle) : ProcessModule(processHandle, engineAddressOffset), FLEngine {
        override val version: String by utf16StringAddress(0x497F67C, 32)
        override val songPosition: Duration by address(0x10A6698) { millis: Long -> millis.milliseconds }
        override val selectedPattern: Int by address(0xF5CCF8)
        override val bpm: Int by address(0xF5FAF0)
        override val isPlaying: Boolean by address(0x1080F0C, Int::toBoolean)

        override val flpPath: String by utf16StringAddress(::flpPathAddress, MAX_PATH.toLong())
        private val flpPathAddress: Long by address(0x10829F0) { address: Long -> address - addressOffset }
        override val flpName: String get() = flpPath.toPath().name

        override val dateCreated: Instant by address(0x1082938) { days: Double ->
            DelphiInstant.fromEpochDays(days, TimeZone.currentSystemDefault()).toUnixInstant()
        }
        override val timeSpent: Duration
            get() {
                if (!isIdle || previousTimeSpent == Duration.ZERO) {
                    previousTimeSpent = Clock.System.now() - timeSpentStartMarker
                }
                return previousTimeSpent
            }
        private val timeSpentStartMarker: Instant by address(0x1082940) { days: Double ->
            DelphiInstant.fromEpochDays(days, TimeZone.currentSystemDefault()).toUnixInstant()
        }
        private val isIdle: Boolean get() = !isPlaying && (isMinimized || !isFocused)
    }

    companion object {
        fun attach(): FLStudio? = WindowHandle.fromExecutableName(executableName)?.let(::FLStudio)

        val FLEngine.formattedTimeSpent: String
            get() = timeSpent.toComponents { hours, minutes, _, _ ->
                when {
                    minutes < 1 -> "less than a minute"
                    minutes == 1 -> "1 minute"
                    hours < 2 -> "$minutes minutes"
                    else -> {
                        val minutesDecimal = (minutes / 6).takeUnlessZero()?.let { ".$it" } ?: ""
                        "$hours$minutesDecimal hours"
                    }
                }
            }
    }
}

expect val FLStudio.Companion.executableName: String