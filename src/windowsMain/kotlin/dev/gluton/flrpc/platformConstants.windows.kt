package dev.gluton.flrpc

import okio.Path
import okio.Path.Companion.toPath
import platform.windows.GetModuleFileNameW

actual val MAX_PATH: Int = platform.windows.MAX_PATH

actual val ICON_EXT: String = "ico"

actual val EXE_PATH: Path by lazy {
    useUtf16StringBuffer(MAX_PATH.toLong()) {
        GetModuleFileNameW(null, it, MAX_PATH.toUInt())
    }.toPath()
}