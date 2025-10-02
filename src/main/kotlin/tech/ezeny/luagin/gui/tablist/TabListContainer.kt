package tech.ezeny.luagin.gui.tablist

class TabListContainer(val guiId: String) {
    private val playerTabListMap = mutableMapOf<String, TabListGui>()
    
    /**
     * 获取或创建玩家的 TAB 列表
     */
    fun getOrCreateTabList(playerName: String): TabListGui {
        return playerTabListMap.getOrPut(playerName) {
            TabListGui(guiId, playerName).apply {
                apply()
            }
        }
    }
    
    /**
     * 获取玩家的 TAB 列表
     */
    fun getTabList(playerName: String): TabListGui? = playerTabListMap[playerName]
    
    /**
     * 移除玩家的 TAB 列表
     */
    fun removeTabList(playerName: String) {
        playerTabListMap[playerName]?.destroy()
        playerTabListMap.remove(playerName)
    }

    /**
     * 容器中 TAB 列表的数量
     */
    fun size(): Int = playerTabListMap.size
}

