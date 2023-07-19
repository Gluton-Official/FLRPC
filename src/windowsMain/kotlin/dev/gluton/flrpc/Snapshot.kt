package dev.gluton.flrpc

import kotlinx.cinterop.CValue
import kotlinx.cinterop.alloc
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.readValue
import kotlinx.cinterop.sizeOf
import okio.Closeable
import platform.windows.CloseHandle
import platform.windows.HANDLE
import platform.windows.INVALID_HANDLE_VALUE
import platform.windows.CreateToolhelp32Snapshot
import platform.windows.MODULEENTRY32W
import platform.windows.Module32FirstW
import platform.windows.Module32NextW
import platform.windows.TH32CS_SNAPMODULE

sealed interface Snapshot : Closeable {
    val handle: HANDLE

    override fun close() {
        if (!CloseHandle(handle).toBoolean()) {
            throw WindowsException("Unable to close handle")
        }
    }
}

class ModuleSnapshot private constructor(override val handle: HANDLE) : Snapshot, Iterable<CValue<MODULEENTRY32W>> {
    private var isClosed = false
    private val allocatedModuleEntries = mutableListOf<MODULEENTRY32W>()

    override fun iterator() = object : Iterator<CValue<MODULEENTRY32W>> {
        val moduleEntry = nativeHeap.alloc<MODULEENTRY32W> {
            dwSize = sizeOf<MODULEENTRY32W>().toUInt()
        }.also(allocatedModuleEntries::add)

        var primedNextEntry = false
        var first = true
        var reachedEnd = false

        override fun hasNext(): Boolean = when {
            isClosed || reachedEnd -> false
            else -> loadNextModule()
        }

        override fun next(): CValue<MODULEENTRY32W> = when {
            isClosed -> error("ModuleSnapshotHandle was closed but next was called on iterator")
            reachedEnd || !loadNextModule() -> throw NoSuchElementException("Called next on ModuleSnapshotIterator without another element")
            else -> {
                primedNextEntry = false
                moduleEntry.readValue()
            }
        }

        private fun loadNextModule(): Boolean = when {
            primedNextEntry -> true
            first -> {
                first = false
                Module32FirstW(handle, moduleEntry.ptr).toBoolean().also { retrievedFirst ->
                    when (retrievedFirst) {
                        true -> primedNextEntry = true
                        false -> reachedEnd = true
                    }
                }
            }
            else -> Module32NextW(handle, moduleEntry.ptr).toBoolean().also { retrievedNext ->
                when (retrievedNext) {
                    true -> primedNextEntry = true
                    false -> reachedEnd = true
                }
            }
        }
    }

    override fun close() {
        isClosed = true
        allocatedModuleEntries.forEach { nativeHeap.free(it.rawPtr) }
        super.close()
    }

    companion object {
        fun create(processId: UInt): ModuleSnapshot? =
            CreateToolhelp32Snapshot(TH32CS_SNAPMODULE.toUInt(), processId)
                .takeIf { it != INVALID_HANDLE_VALUE }
                ?.let(::ModuleSnapshot)
    }
}