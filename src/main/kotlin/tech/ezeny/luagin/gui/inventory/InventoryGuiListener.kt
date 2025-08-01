package tech.ezeny.luagin.gui.inventory

import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.Inventory

object InventoryGuiListener : Listener {
    private val inventoryMap = mutableMapOf<Inventory, InventoryGui>()

    fun registerGui(gui: InventoryGui) {
        inventoryMap[gui.inventory] = gui
    }

    @EventHandler
    fun onInventoryClick(event: InventoryClickEvent) {
        val gui = inventoryMap[event.inventory] ?: return
        gui.handleClick(event)
    }

    @EventHandler
    fun onInventoryOpen(event: InventoryOpenEvent) {
        val gui = inventoryMap[event.inventory] ?: return
        gui.handleOpen(event)
    }

    @EventHandler
    fun onInventoryClose(event: InventoryCloseEvent) {
        val gui = inventoryMap[event.inventory] ?: return
        gui.handleClose(event)
    }
} 