import discord.Discord
import discord.gamesdk.EDiscordLogLevel
import flstudio.FLStudio
import flstudio.FLStudio.Companion.formattedTimeSpent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import okio.use
import kotlin.time.Duration.Companion.seconds

const val APP_ID = 1119828627786322010

@OptIn(ExperimentalForeignApi::class)
fun main() {
    Discord(APP_ID).use useDiscord@{ discord ->
        discord.setLogHook(EDiscordLogLevel.DiscordLogLevel_Debug) { logLevel, message ->
            println("Discord/$logLevel: $message")
        }

        val flStudio = FLStudio.attach() ?: run retry@{
            println("FL Studio is not running yet, waiting 5 seconds...")
            runBlocking { delay(5.seconds) }
            FLStudio.attach() ?: run {
                println("FL Studio still not running! Exiting...")
                return@useDiscord
            }
        }

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
                discord.runCallbacks()

                delay(100)
            }
        }
    }
}