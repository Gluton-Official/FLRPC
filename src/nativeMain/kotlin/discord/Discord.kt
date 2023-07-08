package discord

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
import discord.gamesdk.DiscordCreateFlags_Default
import discord.gamesdk.DiscordCreateParams
import discord.gamesdk.DiscordResult_Ok
import discord.gamesdk.DiscordVersion
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
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.allocPointerTo
import kotlinx.cinterop.cValue
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr

@OptIn(ExperimentalForeignApi::class)
fun Discord(
    clientId: DiscordClientId,
    flags: UInt = DiscordCreateFlags_Default,
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
    achievementVersion: DiscordVersion = DISCORD_ACHIEVEMENT_MANAGER_VERSION,
): CPointerVar<IDiscordCore> = nativeHeap.allocPointerTo<IDiscordCore>().apply {
    val params = cValue<DiscordCreateParams> {
        this.client_id = clientId
        this.flags = flags.toULong()
        this.events = events
        this.event_data = eventData
        this.application_events = applicationEvents
        this.application_version = applicationVersion
        this.user_events = userEvents
        this.user_version = userVersion
        this.image_events = imageEvents
        this.image_version = imageVersion
        this.activity_events = activityEvents
        this.activity_version = activityVersion
        this.relationship_events = relationshipEvents
        this.relationship_version = relationshipVersion
        this.lobby_events = lobbyEvents
        this.lobby_version = lobbyVersion
        this.network_events = networkEvents
        this.network_version = networkVersion
        this.overlay_events = overlayEvents
        this.overlay_version = overlayVersion
        this.storage_events = storageEvents
        this.storage_version = storageVersion
        this.store_events = storeEvents
        this.store_version = storeVersion
        this.voice_events = voiceEvents
        this.voice_version = voiceVersion
        this.achievement_events = achievementEvents
        this.achievement_version = achievementVersion
    }

    check(DiscordCreate(discordVersion, params, this.ptr) == DiscordResult_Ok) {
        "Running Discord application not found!"
    }
}