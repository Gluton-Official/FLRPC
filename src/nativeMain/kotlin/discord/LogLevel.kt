package discord

import discord.gamesdk.DiscordLogLevel_Debug
import discord.gamesdk.DiscordLogLevel_Error
import discord.gamesdk.DiscordLogLevel_Info
import discord.gamesdk.DiscordLogLevel_Warn
import discord.gamesdk.EDiscordLogLevel

val EDiscordLogLevel.name get() = when (this) {
    DiscordLogLevel_Error -> "Error"
    DiscordLogLevel_Warn -> "Warn"
    DiscordLogLevel_Info -> "Info"
    DiscordLogLevel_Debug -> "Debug"
    else -> "$this"
}