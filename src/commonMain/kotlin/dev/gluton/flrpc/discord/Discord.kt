package dev.gluton.flrpc.discord

import dev.gluton.flrpc.util.NativePointerHolder
import dev.gluton.flrpc.util.NativePointerHolder.Companion.use
import discord.gamesdk.DISCORD_ACHIEVEMENT_MANAGER_VERSION
import discord.gamesdk.DISCORD_ACTIVITY_MANAGER_VERSION
import discord.gamesdk.DISCORD_APPLICATION_MANAGER_VERSION
import discord.gamesdk.DISCORD_IMAGE_MANAGER_VERSION
import discord.gamesdk.DISCORD_LOBBY_MANAGER_VERSION
import discord.gamesdk.DISCORD_NETWORK_MANAGER_VERSION
import discord.gamesdk.DISCORD_OVERLAY_MANAGER_VERSION
import discord.gamesdk.DISCORD_RELATIONSHIP_MANAGER_VERSION
import discord.gamesdk.DISCORD_STORAGE_MANAGER_VERSION
import discord.gamesdk.DISCORD_STORE_MANAGER_VERSION
import discord.gamesdk.DISCORD_USER_MANAGER_VERSION
import discord.gamesdk.DISCORD_VERSION
import discord.gamesdk.DISCORD_VOICE_MANAGER_VERSION
import discord.gamesdk.DiscordClientId
import discord.gamesdk.DiscordCreate
import discord.gamesdk.DiscordCreateParams
import discord.gamesdk.DiscordVersion
import discord.gamesdk.EDiscordCreateFlags
import discord.gamesdk.EDiscordLogLevel
import discord.gamesdk.EDiscordResult
import discord.gamesdk.IDiscordAchievementEvents
import discord.gamesdk.IDiscordActivityEvents
import discord.gamesdk.IDiscordApplicationEventsVar
import discord.gamesdk.IDiscordCore
import discord.gamesdk.IDiscordCoreEventsVar
import discord.gamesdk.IDiscordImageEventsVar
import discord.gamesdk.IDiscordLobbyEvents
import discord.gamesdk.IDiscordNetworkEvents
import discord.gamesdk.IDiscordOverlayEvents
import discord.gamesdk.IDiscordRelationshipEvents
import discord.gamesdk.IDiscordStorageEventsVar
import discord.gamesdk.IDiscordStoreEvents
import discord.gamesdk.IDiscordUserEvents
import discord.gamesdk.IDiscordVoiceEvents
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.StableRef
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.asStableRef
import kotlinx.cinterop.invoke
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import okio.Closeable

@Suppress("MemberVisibilityCanBePrivate")
@OptIn(ExperimentalForeignApi::class)
class Discord(
    clientId: DiscordClientId,
    flags: EDiscordCreateFlags = EDiscordCreateFlags.DiscordCreateFlags_Default,
    discordVersion: DiscordVersion = DISCORD_VERSION,
    events: CPointer<IDiscordCoreEventsVar>? = null,
    eventData: COpaquePointer? = null,
    applicationEvents: CPointer<IDiscordApplicationEventsVar>? = null,
    applicationVersion: DiscordVersion = DISCORD_APPLICATION_MANAGER_VERSION,
    userEvents: CPointer<IDiscordUserEvents>? = null,
    userVersion: DiscordVersion = DISCORD_USER_MANAGER_VERSION,
    imageEvents: CPointer<IDiscordImageEventsVar>? = null,
    imageVersion: DiscordVersion = DISCORD_IMAGE_MANAGER_VERSION,
    activityEvents: CPointer<IDiscordActivityEvents>? = null,
    activityVersion: DiscordVersion = DISCORD_ACTIVITY_MANAGER_VERSION,
    relationshipEvents: CPointer<IDiscordRelationshipEvents>? = null,
    relationshipVersion: DiscordVersion = DISCORD_RELATIONSHIP_MANAGER_VERSION,
    lobbyEvents: CPointer<IDiscordLobbyEvents>? = null,
    lobbyVersion: DiscordVersion = DISCORD_LOBBY_MANAGER_VERSION,
    networkEvents: CPointer<IDiscordNetworkEvents>? = null,
    networkVersion: DiscordVersion = DISCORD_NETWORK_MANAGER_VERSION,
    overlayEvents: CPointer<IDiscordOverlayEvents>? = null,
    overlayVersion: DiscordVersion = DISCORD_OVERLAY_MANAGER_VERSION,
    storageEvents: CPointer<IDiscordStorageEventsVar>? = null,
    storageVersion: DiscordVersion = DISCORD_STORAGE_MANAGER_VERSION,
    storeEvents: CPointer<IDiscordStoreEvents>? = null,
    storeVersion: DiscordVersion = DISCORD_STORE_MANAGER_VERSION,
    voiceEvents: CPointer<IDiscordVoiceEvents>? = null,
    voiceVersion: DiscordVersion = DISCORD_VOICE_MANAGER_VERSION,
    achievementEvents: CPointer<IDiscordAchievementEvents>? = null,
    achievementVersion: DiscordVersion = DISCORD_ACHIEVEMENT_MANAGER_VERSION
) : NativePointerHolder<IDiscordCore>, Closeable {

    override val nativePointer: CPointer<IDiscordCore>
    private val stableSelfRef = StableRef.create(this)

    val activityManager: ActivityManager by lazy {
        use {
            ActivityManager(get_activity_manager!!.invoke(ptr)!!)
        }
    }

    init {
        val core = nativeHeap.allocPointerTo<IDiscordCore>()
        memScoped {
            val params = alloc<DiscordCreateParams>()
            params.client_id = clientId
            params.flags = flags.value.toULong()
            params.events = events
            params.event_data = eventData
            params.application_events = applicationEvents
            params.application_version = applicationVersion
            params.user_events = userEvents
            params.user_version = userVersion
            params.image_events = imageEvents
            params.image_version = imageVersion
            params.activity_events = activityEvents
            params.activity_version = activityVersion
            params.relationship_events = relationshipEvents
            params.relationship_version = relationshipVersion
            params.lobby_events = lobbyEvents
            params.lobby_version = lobbyVersion
            params.network_events = networkEvents
            params.network_version = networkVersion
            params.overlay_events = overlayEvents
            params.overlay_version = overlayVersion
            params.storage_events = storageEvents
            params.storage_version = storageVersion
            params.store_events = storeEvents
            params.store_version = storeVersion
            params.voice_events = voiceEvents
            params.voice_version = voiceVersion
            params.achievement_events = achievementEvents
            params.achievement_version = achievementVersion

            check(DiscordCreate(discordVersion, params.ptr, core.ptr) == EDiscordResult.DiscordResult_Ok) {
                "Running Discord application not found!"
            }
        }
        nativePointer = core.value!!
    }

    override fun close() = destroy()

    fun destroy(): Unit = use { destroy!!.invoke(ptr) }

    fun runCallbacks(): EDiscordResult = use {
        run_callbacks!!.invoke(ptr)
    }

    private var logHook: ((EDiscordLogLevel, String?) -> Unit)? = null
    fun setLogHook(minLogLevel: EDiscordLogLevel, callback: ((EDiscordLogLevel, String?) -> Unit)?): Unit = use {
        // TODO: try to use StableRef with the callback
        logHook = callback
        set_log_hook!!.invoke(ptr, minLogLevel, stableSelfRef.asCPointer(), logHook?.run {
            staticCFunction { selfPointer, logLevel: EDiscordLogLevel, message ->
                val discord = selfPointer!!.asStableRef<Discord>().get()
                discord.logHook?.invoke(logLevel, message?.toKString())
            }
        })
    }
}