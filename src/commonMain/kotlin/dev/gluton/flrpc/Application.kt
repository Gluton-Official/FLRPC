package dev.gluton.flrpc

import okio.use

open class Application(private val windowHandle: WindowHandle) {
    val processId: UInt = windowHandle.processId?.takeUnlessZero() ?: error("Unable to get process id")
    val windowTitle: String? by windowHandle::title

    val isFocused: Boolean by windowHandle::isFocused
    val isMinimized: Boolean by windowHandle::isMinimized
    val isRunning: Boolean get() = openProcess(processId, ProcessFlags.Info)?.use(ProcessHandle::isRunning) ?: false
}