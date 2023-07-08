package windows

import close
import kotlinx.cinterop.ExperimentalForeignApi
import okio.Closeable
import platform.windows.HANDLE

@OptIn(ExperimentalForeignApi::class)
open class WindowsProcess constructor(protected val handle: HANDLE) : Closeable {
    override fun close() {
        if (!handle.close()) {
            throw RuntimeException("Unable to close handle for ${this::class.qualifiedName}")
        }
    }
}