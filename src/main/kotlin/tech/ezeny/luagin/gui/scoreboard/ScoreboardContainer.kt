package tech.ezeny.luagin.gui.scoreboard

class ScoreboardContainer(val guiId: String) {
    private val playerScoreboardMap = mutableMapOf<String, ScoreboardGui>()
    
    /**
     * 获取或创建玩家的记分板
     */
    fun getOrCreateScoreboard(playerName: String, title: String): ScoreboardGui {
        return playerScoreboardMap.getOrPut(playerName) {
            ScoreboardGui(title, guiId)
        }
    }
    
    /**
     * 获取玩家的记分板
     */
    fun getScoreboard(playerName: String): ScoreboardGui? = playerScoreboardMap[playerName]
    
    /**
     * 移除玩家的记分板
     */
    fun removeScoreboard(playerName: String) {
        playerScoreboardMap[playerName]?.destroy()
        playerScoreboardMap.remove(playerName)
    }

    /**
     * 获取容器中记分板的数量
     */
    fun size(): Int = playerScoreboardMap.size
}
