package dev.gluton.flrpc

import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.allocArrayOfPointersTo
import kotlinx.cinterop.get
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.staticCFunction
import dmikushin.tray.tray as TrayMenu
import dmikushin.tray.tray_exit as trayExit
import dmikushin.tray.tray_menu as TrayMenuItem

val ICON_PATH = "flrpc.$ICON_EXT"

val tray = nativeHeap.run {
    alloc<TrayMenu> {
        icon = allocArrayOf(ICON_PATH.encodeToByteArray())
        tooltip = allocArrayOf("FLRPC".encodeToByteArray())

        val menuItems = allocArrayOfPointersTo(
            alloc<TrayMenuItem> {
                text = allocArrayOf("Quit".encodeToByteArray())
                cb = staticCFunction { _ -> trayExit() }
            },
            null
        )

        menu = menuItems[0]
    }
}