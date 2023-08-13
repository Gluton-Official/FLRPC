package dev.gluton.flrpc.discord

import dev.gluton.flrpc.util.NativePointerHolder
import dev.gluton.flrpc.util.NativePointerHolder.Companion.use
import discord.gamesdk.DiscordActivity
import discord.gamesdk.EDiscordActivityPartyPrivacy
import discord.gamesdk.IDiscordActivityManager
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.cstr
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.datetime.Instant

@OptIn(ExperimentalForeignApi::class)
value class ActivityManager internal constructor(
    override val nativePointer: CPointer<IDiscordActivityManager>,
): NativePointerHolder<IDiscordActivityManager> {

    fun updateActivity(
        state: String? = null,
        details: String? = null,
        timestampStart: Instant? = null,
        timestampEnd: Instant? = null,
        largeImageId: String? = null,
        largeImageText: String? = null,
        smallImageId: String? = null,
        smallImageText: String? = null,
        partyId: String? = null,
        partyMaxSize: Int? = null,
        partyPrivacy: EDiscordActivityPartyPrivacy? = null,
        isInstance: Boolean? = null,
    ) = use {
        memScoped {
            val activity = alloc<DiscordActivity> activity@{
                state?.cstr?.place(this@activity.state)
                details?.cstr?.place(this@activity.details)
                timestamps.apply {
                    timestampStart?.apply { start = epochSeconds }
                    timestampEnd?.apply { end = epochSeconds }
                }
                assets.apply {
                    largeImageId?.cstr?.place(large_image)
                    largeImageText?.cstr?.place(large_text)
                    smallImageId?.cstr?.place(small_image)
                    smallImageText?.cstr?.place(small_text)
                }
                party.apply {
                    partyId?.cstr?.place(id)
                    size.apply {
                        partyMaxSize?.let { max_size = it }
                    }
                    partyPrivacy?.let { privacy = it }
                }
                isInstance?.let { instance = it }
            }
            update_activity!!.invoke(ptr, activity.ptr, null, null)
        }
    }
}