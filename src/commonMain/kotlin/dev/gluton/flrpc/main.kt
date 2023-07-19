package dev.gluton.flrpc

import dev.gluton.flrpc.FLStudio.Companion.formattedTimeSpent
import dev.gluton.flrpc.discord.Discord
import discord.gamesdk.EDiscordLogLevel
import korlibs.logger.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.use
import kotlin.time.Duration.Companion.seconds

val logger = Logger("FLRPC")

const val APP_ID = 1119828627786322010

fun main() {
    logger.trace { "Hello FL Studio user" }
    Discord(APP_ID).use useDiscord@{ discord ->
        logger.trace { "Init Discord" }

        discord.setLogHook(EDiscordLogLevel.DiscordLogLevel_Debug) { logLevel, message ->
            println("Discord/$logLevel: $message")
        }

        val flStudio = FLStudio.attach() ?: run retry@{
            println("FL Studio is not running yet, waiting 5 seconds...")
            runBlocking { delay(5.seconds) }
            FLStudio.attach() ?: run failed@{
                println("FL Studio still not running! Exiting...")
                return@useDiscord
            }
        }
        logger.trace { "Init FL" }

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
        }

        runBlocking {
            while (true) {
                if (!flStudio.isRunning) break

                updateRPC()
                logger.trace { "Updated RPC" }
                discord.runCallbacks()

                delay(1.seconds)
            }
        }
    }
}