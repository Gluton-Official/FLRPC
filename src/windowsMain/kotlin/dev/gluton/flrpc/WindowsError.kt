package dev.gluton.flrpc

import dev.gluton.flrpc.util.useUtf16StringBuffer
import platform.windows.DWORD
import platform.windows.FORMAT_MESSAGE_FROM_SYSTEM
import platform.windows.FORMAT_MESSAGE_IGNORE_INSERTS
import platform.windows.FormatMessageW
import platform.windows.GetLastError
import platform.windows.LANG_USER_DEFAULT

open class WindowsException(message: String? = null) : RuntimeException(
    run {
        val (errorCode, errorMessage) = getLastWindowsError()
        when (message) {
            null -> "$errorCode ($errorMessage)"
            else -> "$errorCode ($errorMessage): $message"
        }
    }
)

fun getLastWindowsError(): Pair<DWORD, String> {
    val errorCode = GetLastError()
    return errorCode to useUtf16StringBuffer(256) {
        FormatMessageW(
            (FORMAT_MESSAGE_FROM_SYSTEM or FORMAT_MESSAGE_IGNORE_INSERTS).toUInt(),
            null,
            errorCode,
            LANG_USER_DEFAULT.toUInt(),
            it,
            256u,
            null
        )
    }.trimEnd().trimEnd('.')
}
