package discord

import discord.gamesdk.DiscordResult_Ok
import discord.gamesdk.EDiscordResult

open class DiscordException(val result: EDiscordResult) : Throwable()

inline fun EDiscordResult.asResult(): Result<EDiscordResult> = when (this) {
    DiscordResult_Ok -> Result.success(this)
    else -> Result.failure(DiscordException(this))
}
