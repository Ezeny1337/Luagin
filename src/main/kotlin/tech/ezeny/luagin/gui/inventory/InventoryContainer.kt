package tech.ezeny.luagin.gui.inventory

class InventoryContainer(val guiId: String) {
    private val playerGuiMap = mutableMapOf<String, InventoryGui>()

    fun getOrCreateInventory(playerName: String, title: String, size: Int, storable: Boolean): InventoryGui {
        return playerGuiMap.getOrPut(playerName) {
            InventoryGui(title, size, guiId, storable)
        }
    }

    fun getInventory(playerName: String): InventoryGui? = playerGuiMap[playerName]

    fun removeInventory(playerName: String) {
        val gui = playerGuiMap[playerName]
        if (gui != null) {
            gui.close(playerName)
            if (gui.storable) {
                gui.saveStoredItems(playerName)
            } else {
                gui.returnItemsToPlayer(playerName)
            }
        }
        playerGuiMap.remove(playerName)
    }

    /**
     * 获取容器中 Inventory 的数量
     */
    fun size(): Int = playerGuiMap.size
}