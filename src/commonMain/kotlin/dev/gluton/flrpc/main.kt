package dev.gluton.flrpc

import dev.gluton.flrpc.FLStudio.Companion.formattedTimeSpent
import dev.gluton.flrpc.discord.Discord
import discord.gamesdk.EDiscordLogLevel
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.use
import platform.posix.STDERR_FILENO
import platform.posix.STDOUT_FILENO
import platform.posix.dup2
import platform.posix.errno
import platform.posix.freopen
import platform.posix.getenv
import platform.posix.stdout
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import dmikushin.tray.tray_exit as trayExit
import dmikushin.tray.tray_init as trayInit
import dmikushin.tray.tray_loop as trayLoop

const val APP_ID = 1119828627786322010

fun main() {
    if (getenv("LOG_FLRPC_OUTPUT")?.toKString()?.lowercase() != "console") {
        val logFilePath = EXE_PATH.parent?.resolve("FLRPC.log").toString()
        freopen(logFilePath, "w", stdout) ?: FLRpcLogger.error { "Failed reopening to stdout: $errno" }
        dup2(STDOUT_FILENO, STDERR_FILENO).takeUnless { it == -1 } ?: FLRpcLogger.error { "Failed duplicating stdout to stderr: $errno" }
        FLRpcLogger.debug { "Redirected output to log file: $logFilePath" }
    }

    trayInit(tray.ptr)
    FLRpcLogger.trace { "Initialized tray menu" }

    Discord(APP_ID).use discord@{ discord ->
        FLRpcLogger.trace { "Connected to Discord RPC" }
        discord.setLogHook(EDiscordLogLevel.DiscordLogLevel_Debug) { logLevel, message ->
            println("Discord/$logLevel: $message")
        }

        val flStudio = FLStudio.attach() ?: run retry@{
            println("FL Studio is not running yet, waiting 5 seconds...")
            runBlocking { delay(5.seconds) }
            FLStudio.attach() ?: run failed@{
                println("FL Studio still not running! Exiting...")
                return@discord
            }
        }
        FLRpcLogger.trace { "Attached to FL Studio" }

        fun updateRPC() {
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
            FLRpcLogger.trace { "Updated RPC" }
        }

        runBlocking {
            var lastUpdateRPCTime = Instant.DISTANT_PAST
            while (true) {
                if (!flStudio.isRunning) {
                    FLRpcLogger.debug { "FL Studio stopped running" }
                    break
                }
                if (trayLoop(false.toInt()).toBoolean()) {
                    FLRpcLogger.debug { "Tray menu was closed" }
                    break
                }

                if (Clock.System.now() - lastUpdateRPCTime >= 1.seconds) {
                    updateRPC()
                    lastUpdateRPCTime = Clock.System.now()
                }

                discord.runCallbacks()

                delay(100.milliseconds)
            }
        }
    }

    trayExit()
    FLRpcLogger.trace { "Closing" }
}