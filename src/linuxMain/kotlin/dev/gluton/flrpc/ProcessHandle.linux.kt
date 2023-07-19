package dev.gluton.flrpc

actual fun openProcess(processId: UInt, flags: ProcessFlags): ProcessHandle? = TODO()
actual fun findModuleAddressByName(processId: UInt, name: String): Long? = TODO()

actual val ProcessFlags.Companion.ReadMemory: ProcessFlags get() = TODO()
actual val ProcessFlags.Companion.Info: ProcessFlags get() = TODO()