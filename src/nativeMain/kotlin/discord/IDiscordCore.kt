@file:OptIn(ExperimentalForeignApi::class)

package discord

import discord.gamesdk.DiscordLogLevel_Debug
import discord.gamesdk.EDiscordLogLevel
import discord.gamesdk.IDiscordActivityManager
import discord.gamesdk.IDiscordCore
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.invoke
import kotlinx.cinterop.pointed
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

inline val CPointerVar<IDiscordCore>.activityManager: CPointer<IDiscordActivityManager>
    get() = pointed!!.get_activity_manager!!.invoke(value!!)!!

inline fun CPointerVar<IDiscordCore>.runCallbacks() = pointed!!.run_callbacks!!.invoke(value!!)

fun CPointerVar<IDiscordCore>.setLogHook(callback: SetLogHookCallback) {
    setLogHookCallback = callback
    pointed!!.set_log_hook!!.invoke(
        value!!,
        DiscordLogLevel_Debug,
        null,
        staticCFunction { _, logLevel, message ->
            setLogHookCallback?.invoke(logLevel, message?.toKString())
        }
    )
}
private var setLogHookCallback: SetLogHookCallback? = null

typealias SetLogHookCallback = (logLevel: EDiscordLogLevel, message: String?) -> Unit

// TODO: make Rust's borrow checker like coroutines
@OptIn(DelicateCoroutinesApi::class)
inline fun CPointerVar<IDiscordCore>.launchCallbackProcessor(
    crossinline finished: () -> Boolean = { false },
): Job {
    val runCallbacks = pointed?.run_callbacks!!
    return GlobalScope.launch {
        while (true) {
            runCallbacks(value)
            if (finished()) break
        }
    }
}