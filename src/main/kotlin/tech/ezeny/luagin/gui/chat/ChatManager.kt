package tech.ezeny.luagin.gui.chat

import java.util.concurrent.ConcurrentHashMap

object ChatManager {
    
    // 存储玩家的聊天前缀
    private val playerPrefixes = ConcurrentHashMap<String, String>()
    
    // 存储玩家的名字颜色
    private val playerNameColors = ConcurrentHashMap<String, String>()
    
    // 存储玩家的名字渐变颜色配置
    private val playerNameGradients = ConcurrentHashMap<String, List<String>>()
    
    /**
     * 设置玩家的聊天前缀
     * 
     * @param playerName 玩家名
     * @param prefix 前缀文本
     */
    fun setPlayerPrefix(playerName: String, prefix: String) {
        if (prefix.isEmpty()) {
            playerPrefixes.remove(playerName)
        } else {
            playerPrefixes[playerName] = prefix
        }
    }
    
    /**
     * 获取玩家的聊天前缀
     * 
     * @param playerName 玩家名
     * @return 前缀文本
     */
    fun getPlayerPrefix(playerName: String): String? {
        return playerPrefixes[playerName]
    }
    
    /**
     * 设置玩家的名字颜色
     * 
     * @param playerName 玩家名
     * @param color 颜色代码
     */
    fun setPlayerNameColor(playerName: String, color: String) {
        if (color.isEmpty()) {
            playerNameColors.remove(playerName)
        } else {
            playerNameColors[playerName] = color
        }
    }
    
    /**
     * 获取玩家的名字颜色
     * 
     * @param playerName 玩家名
     * @return 颜色代码
     */
    fun getPlayerNameColor(playerName: String): String? {
        return playerNameColors[playerName]
    }
    
    /**
     * 设置玩家的名字渐变颜色
     * 
     * @param playerName 玩家名
     * @param colors 颜色列表
     */
    fun setPlayerNameGradient(playerName: String, colors: List<String>) {
        if (colors.isEmpty()) {
            playerNameGradients.remove(playerName)
        } else {
            // 清除单色设置，因为渐变优先
            playerNameColors.remove(playerName)
            playerNameGradients[playerName] = colors
        }
    }
    
    /**
     * 获取玩家的名字渐变颜色配置
     * 
     * @param playerName 玩家名
     * @return 颜色列表
     */
    fun getPlayerNameGradient(playerName: String): List<String>? {
        return playerNameGradients[playerName]
    }
    
    /**
     * 清除玩家的所有聊天设置
     * 
     * @param playerName 玩家名
     */
    fun clearPlayerSettings(playerName: String) {
        playerPrefixes.remove(playerName)
        playerNameColors.remove(playerName)
        playerNameGradients.remove(playerName)
    }
}
