package dev.gluton.flrpc

import dev.gluton.flrpc.util.EXE_PATH
import korlibs.logger.AnsiEscape
import korlibs.logger.AnsiEscape.Companion.blue
import korlibs.logger.AnsiEscape.Companion.color
import korlibs.logger.AnsiEscape.Companion.red
import korlibs.logger.AnsiEscape.Companion.white
import korlibs.logger.AnsiEscape.Companion.yellow
import korlibs.logger.Console
import korlibs.logger.Logger
import kotlinx.cinterop.toKString
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import okio.buffer
import okio.use
import platform.posix.getenv
import kotlin.time.Duration.Companion.nanoseconds

private val logFilePath = EXE_PATH.parent?.resolve("FLRPC.log")?.also {
    FileSystem.SYSTEM.delete(it, mustExist = false)
}

val FLRpcLogger = Logger("FLRPC").apply {
    val minLevel = getenv("LOG_FLRPC")?.toKString()?.let(Logger.Level::get) ?: Logger.Level.INFO
    output = object : Logger.Output {
        override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
            val message = msg.toString()
            val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
            if (minLevel.index >= Logger.Level.DEBUG.index) {
                writeToConsole(logger, level, time, message)
            }
            if (logFilePath != null && level.index < Logger.Level.TRACE.index) {
                writeToFile(logFilePath, logger, level, time, message)
            }
        }
    }
}

val DiscordLogger = Logger("Discord").apply {
    val minLevel = getenv("LOG_DISCORD")?.toKString()?.let(Logger.Level::get) ?: Logger.Level.INFO
    output = object : Logger.Output {
        override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
            val message = msg.toString()
            val time = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).time
            if (minLevel.index >= Logger.Level.DEBUG.index) {
                writeToConsole(logger, level, time, message)
            }
            if (logFilePath != null) {
                writeToFile(logFilePath, logger, level, time, message)
            }
        }
    }
}

private fun writeToConsole(logger: Logger, level: Logger.Level, time: LocalTime, message: String) {
    val formattedMessage = formatLogMessage(logger, level, time, message, withColor = true)
    when (level) {
        Logger.Level.ERROR -> Console.error(formattedMessage)
        Logger.Level.WARN -> Console.warn(formattedMessage)
        else -> Console.log(formattedMessage)
    }
}

private fun writeToFile(path: Path, logger: Logger, level: Logger.Level, time: LocalTime, message: String) {
    val formattedMessage = formatLogMessage(logger, level, time, message)
    with(FileSystem.SYSTEM) {
        fun writeMessage(sink: BufferedSink) = sink.apply {
            writeUtf8(formattedMessage)
            writeUtf8("\n")
        }

        if (exists(path)) {
            appendingSink(path, mustExist = true).buffer().use(::writeMessage)
        } else {
            write(path, mustCreate = true, ::writeMessage)
        }
    }
}

private fun formatLogMessage(
    logger: Logger,
    level: Logger.Level,
    time: LocalTime,
    message: String,
    withColor: Boolean = false
): String = buildString {
    append('[')
    appendFormattedTime(time, withColor)
    append("] ", logger.name, '/')
    appendLevel(level, withColor)
    append(": ")
    appendMessage(message, level, withColor)
}

private inline fun StringBuilder.appendFormattedTime(time: LocalTime, withColor: Boolean) = with(time) {
    val (paddedHour, paddedMinute, paddedSecond) = listOf(hour, minute, second).map {
        with(it.toString().padStart(2, '0')) {
            if (withColor) yellow else this
        }
    }
    val paddedMilli = with(nanosecond.nanoseconds.inWholeMilliseconds.toString().padStart(3, '0')) {
        if (withColor) white else this
    }
    append(paddedHour, ':', paddedMinute, ':', paddedSecond, ':', paddedMilli)
}

private inline fun StringBuilder.appendLevel(level: Logger.Level, withColor: Boolean) = append(level.name.run {
    if (withColor) {
        when (level) {
            Logger.Level.TRACE -> color(AnsiEscape.Color.WHITE, bright = true)
            Logger.Level.DEBUG -> blue
            Logger.Level.INFO -> color(AnsiEscape.Color.GREEN, bright = true)
            Logger.Level.WARN -> color(AnsiEscape.Color.YELLOW, bright = true)
            Logger.Level.ERROR -> color(AnsiEscape.Color.RED, bright = true)
            Logger.Level.FATAL -> red
            Logger.Level.NONE -> this
        }
    } else this
})

private inline fun StringBuilder.appendMessage(message: String, level: Logger.Level, withColor: Boolean) = append(message.run {
    if (withColor) {
        when (level) {
            Logger.Level.ERROR -> color(AnsiEscape.Color.RED, bright = true)
            Logger.Level.FATAL -> red
            else -> this
        }
    } else this
})