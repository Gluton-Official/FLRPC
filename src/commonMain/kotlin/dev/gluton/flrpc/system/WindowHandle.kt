package dev.gluton.flrpc.system

interface WindowHandle {
    val title: String?
    val processId: UInt?
    val isVisible: Boolean
    val isMinimized: Boolean
    val isFocused: Boolean

    companion object
}

expect fun WindowHandle.Companion.fromExecutableName(executableName: String): WindowHandle?