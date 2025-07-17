package tech.ezeny.luagin.gui

class GuiContainer(val guiId: String) {
    private val playerGuiMap = mutableMapOf<String, InventoryGui>()

    fun getOrCreateInventory(playerName: String, title: String, size: Int, storable: Boolean): InventoryGui {
        return playerGuiMap.getOrPut(playerName) {
            InventoryGui(title, size, guiId, storable)
        }
    }

    fun getInventory(playerName: String): InventoryGui? = playerGuiMap[playerName]
} 