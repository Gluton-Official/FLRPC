package dev.gluton.flrpc

import dev.gluton.flrpc.FLStudio.Companion.formattedTimeSpent
import dev.gluton.flrpc.discord.Discord
import discord.gamesdk.EDiscordCreateFlags
import discord.gamesdk.EDiscordLogLevel
import korlibs.logger.Console
import korlibs.logger.Logger
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CArrayPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeNullPtr
import kotlinx.cinterop.plus
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.rawValue
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.use
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.dup2
import platform.posix.environ
import platform.posix.fopen
import platform.posix.freopen
import platform.posix.getenv
import platform.posix.stderr
import platform.posix.stdout
import kotlin.time.Duration.Companion.seconds
import dmikushin.tray.tray_exit as trayExit
import dmikushin.tray.tray_init as trayInit
import dmikushin.tray.tray_loop as trayLoop

val logger = Logger("FLRPC").apply {
    output = object : Logger.Output {
        override fun output(logger: Logger, level: Logger.Level, msg: Any?) {
            val formatted = "${logger.name}/${level.name.lowercase().replaceFirstChar(Char::uppercaseChar)}: $msg"
            when (level) {
                Logger.Level.ERROR -> Console.error(formatted)
                Logger.Level.WARN -> Console.warn(formatted)
                else -> Console.log(formatted)
            }
        }
    }
}

const val APP_ID = 1119828627786322010

fun main() {
    if (getenv("LOG_FLRPC_OUTPUT")?.toKString()?.lowercase() != "console") {
        val logFilePath = EXE_PATH.parent?.resolve("FLRPC.log").toString()
        logger.debug { "Redirecting output to log file: $logFilePath" }
        freopen(logFilePath, "a", stdout)
        dup2(STDOUT_FILENO, STDERR_FILENO)
    }

    logger.trace { "Initializing tray menu" }
    trayInit(tray.ptr)

    logger.trace { "Connecting to Discord RPC" }
    Discord(APP_ID).use useDiscord@{ discord ->
        discord.setLogHook(EDiscordLogLevel.DiscordLogLevel_Debug) { logLevel, message ->
            println("Discord/$logLevel: $message")
        }

        logger.trace { "Attaching to FL Studio" }
        val flStudio = FLStudio.attach() ?: run retry@{
            println("FL Studio is not running yet, waiting 5 seconds...")
            runBlocking { delay(5.seconds) }
            FLStudio.attach() ?: run failed@{
                println("FL Studio still not running! Exiting...")
                return@useDiscord
            }
        }

        fun updateRPC() {
            logger.trace { "Updating RPC" }
            flStudio.useEngine {
                discord.activityManager.updateActivity(
                    details = flpName,
                    state = formattedTimeSpent,
                    largeImageId = "fl",
                    largeImageText = flStudio.name,
                    isInstance = false,
                )
            }.onFailure { throwable ->
                error("Failed to use engine${throwable.message?.let { ": $it" } ?: ""}")
            }
        }

        runBlocking {
            var lastUpdateRPCTime = Instant.DISTANT_PAST
            while (true) {
                if (!flStudio.isRunning) {
                    logger.debug { "FL Studio stopped running" }
                    break
                }
                if (trayLoop(false.toInt()).toBoolean()) {
                    logger.debug { "Tray menu was closed" }
                    break
                }

                if (Clock.System.now() - lastUpdateRPCTime >= 1.seconds) {
                    updateRPC()
                    lastUpdateRPCTime = Clock.System.now()
                }

                discord.runCallbacks()
            }
        }
    }

    trayExit()
    logger.trace { "Closing" }
}