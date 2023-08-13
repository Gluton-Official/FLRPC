package dev.gluton.flrpc

import dev.gluton.flrpc.FLStudio.Companion.formattedTimeSpent
import dev.gluton.flrpc.discord.Discord
import dev.gluton.flrpc.util.toBoolean
import dev.gluton.flrpc.util.toInt
import discord.gamesdk.EDiscordLogLevel
import korlibs.logger.Logger
import kotlinx.cinterop.ptr
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import okio.use
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import dmikushin.tray.tray_exit as trayExit
import dmikushin.tray.tray_init as trayInit
import dmikushin.tray.tray_loop as trayLoop

const val APP_ID = 1119828627786322010

fun main() = try {
    trayInit(Tray.ptr)
    FLRpcLogger.trace { "Initialized tray menu" }

    Discord(APP_ID).use discord@{ discord ->
        FLRpcLogger.trace { "Connected to Discord RPC" }
        discord.setLogHook(EDiscordLogLevel.DiscordLogLevel_Debug) { logLevel, message ->
            DiscordLogger.log(Logger.Level.get(logLevel.name.substringAfter('_'))) { message }
        }

        val flStudio = FLStudio.attach() ?: run retry@{
            FLRpcLogger.info { "FL Studio is not running yet, waiting 5 seconds..." }
            runBlocking { delay(5.seconds) }

            FLStudio.attach() ?: run failed@{
                FLRpcLogger.info { "FL Studio still not running! Exiting..." }
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
                FLRpcLogger.error {
                    buildString {
                        append("Failed to use engine")
                        throwable.message?.let { append(": ", it) }
                        appendLine()
                        append(throwable.stackTraceToString())
                    }
                }
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
} catch (e: Throwable) {
    FLRpcLogger.error(e::stackTraceToString)
    throw e
}