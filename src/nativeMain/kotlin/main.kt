import discord.Discord
import discord.activityManager
import discord.gamesdk.DiscordActivity
import discord.name
import discord.runCallbacks
import discord.setLogHook
import discord.updateActivity
import flstudio.FLStudio
import flstudio.FLStudio.Companion.formattedTimeSpent
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.seconds

const val APP_ID = 1119828627786322010

@OptIn(ExperimentalForeignApi::class)
fun main() {
    val discord = Discord(APP_ID)
    discord.setLogHook { logLevel, message ->
        println("Discord/${logLevel.name}: $message")
    }

    val flStudio = FLStudio.attach() ?: run {
        println("FL Studio is not running yet, waiting for 5 seconds...")
        runBlocking {
            // give a few seconds for FL to start before trying to attach
            delay(5.seconds)
        }
        FLStudio.attach() ?: run {
            println("FL Studio still not running! Exiting...")
            return
        }
    }

    fun updateRPC() {
        memScoped {
            val activity = alloc<DiscordActivity> {
                flStudio.useEngine {
                    details.write(flpName)
                    state.write(formattedTimeSpent)
                    assets.apply {
                        large_image.write("fl")
                        large_text.write(flStudio.name)
                    }
                    // TODO: use song position as time elapsed?
                    instance = false
                }.onFailure { throwable ->
                    error("Failed to use engine${throwable.message?.let { ": $it" } ?: ""}")
                }
            }
            discord.activityManager.updateActivity(activity)
        }
    }

    while (true) {
        discord.runCallbacks()
        if (!flStudio.isRunning) break
        updateRPC()
        runBlocking {
            delay(5.seconds)
        }
    }

    discord.delete()
}