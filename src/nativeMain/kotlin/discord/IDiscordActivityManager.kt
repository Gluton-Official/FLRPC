package discord

import discord.gamesdk.DiscordActivity
import discord.gamesdk.IDiscordActivityManager
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr

@OptIn(ExperimentalForeignApi::class)
inline fun CPointer<IDiscordActivityManager>.updateActivity(activity: DiscordActivity) {
    this.pointed.update_activity!!.invoke(this, activity.ptr, null, null)
}