package dev.gluton.flrpc

import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.allocArrayOfPointersTo
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.staticCFunction
import dmikushin.tray.tray as Tray
import dmikushin.tray.tray_exit as exitTray
import dmikushin.tray.tray_menu as TrayItem

val ICON_PATH = "flrpc.$ICON_EXT"

val tray = nativeHeap.alloc<Tray> tray@{
    icon = nativeHeap.allocArrayOf(ICON_PATH.encodeToByteArray())
    tooltip = nativeHeap.allocArrayOf("FLRPC".encodeToByteArray())

    val menuItems = nativeHeap.allocArrayOfPointersTo(
        nativeHeap.alloc<TrayItem> {
            text = nativeHeap.allocArrayOf("Quit".encodeToByteArray())
            cb = staticCFunction { _ -> exitTray() }
        },
        null
    )

    menu = menuItems[0]
}