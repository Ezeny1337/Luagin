package tech.ezeny.luagin.gui.tablist

import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.ColorUtils
import org.bukkit.ChatColor as BukkitChatColor

class TabListGui(
    val guiId: String,
    private val ownerName: String
) {
    private var scoreboard: Scoreboard = Bukkit.getScoreboardManager()!!.newScoreboard
    private var header: String = ""
    private var footer: String = ""
    private val playerTeams = mutableMapOf<String, Team>() // 玩家名称 -> Team
    private var isApplied = false
    
    // 动画任务
    private var animationTask: BukkitRunnable? = null
    
    /**
     * 应用 TAB 列表设置到玩家
     */
    fun apply() {
        val player = Bukkit.getPlayer(ownerName) ?: return
        player.scoreboard = scoreboard
        updateHeaderFooter()
        isApplied = true
    }
    
    /**
     * 重置 TAB 列表
     */
    fun reset() {
        val player = Bukkit.getPlayer(ownerName) ?: return
        player.scoreboard = Bukkit.getScoreboardManager()!!.mainScoreboard
        player.playerListHeader = ""
        player.playerListFooter = ""
        isApplied = false
    }
    
    /**
     * 设置 Header
     */
    fun setHeader(text: String) {
        header = text
        if (isApplied) {
            updateHeaderFooter()
        }
    }
    
    /**
     * 设置 Footer
     */
    fun setFooter(text: String) {
        footer = text
        if (isApplied) {
            updateHeaderFooter()
        }
    }
    
    /**
     * 更新玩家的 Header 和 Footer
     */
    private fun updateHeaderFooter() {
        val player = Bukkit.getPlayer(ownerName) ?: return
        val colorizedHeader = ColorUtils.formatString(header)
        val colorizedFooter = ColorUtils.formatString(footer)
        player.playerListHeader = colorizedHeader
        player.playerListFooter = colorizedFooter
    }
    
    /**
     * 设置玩家的前缀
     * @param targetPlayerName 要设置前缀的玩家名称
     * @param prefix 前缀文本
     */
    fun setPlayerPrefix(targetPlayerName: String, prefix: String) {
        val team = getOrCreateTeam(targetPlayerName)
        team.prefix = ColorUtils.formatString(prefix)
    }
    
    /**
     * 设置玩家的后缀
     * @param targetPlayerName 要设置后缀的玩家名称
     * @param suffix 后缀文本
     */
    fun setPlayerSuffix(targetPlayerName: String, suffix: String) {
        val team = getOrCreateTeam(targetPlayerName)
        team.suffix = ColorUtils.formatString(suffix)
    }
    
    /**
     * 设置玩家名字的颜色
     * @param targetPlayerName 要设置颜色的玩家名称
     * @param color 颜色（支持颜色代码如 "§c"、"&c" 或颜色名称如 "RED"）
     */
    fun setPlayerColor(targetPlayerName: String, color: String) {
        val team = getOrCreateTeam(targetPlayerName)
        val chatColor = parseColor(color)
        if (chatColor != null) {
            team.color = chatColor
        }
    }
    
    /**
     * 解析传统颜色字符串
     * @param colorStr 颜色字符串
     * @return ChatColor 或 null
     */
    private fun parseColor(colorStr: String): BukkitChatColor? {
        return try {
            when {
                // 处理颜色代码 (§c 格式)
                colorStr.startsWith("§") && colorStr.length == 2 -> {
                    BukkitChatColor.getByChar(colorStr[1])
                }
                // 处理颜色代码 (&c 格式)
                colorStr.startsWith("&") && colorStr.length == 2 -> {
                    BukkitChatColor.getByChar(colorStr[1])
                }
                // 处理颜色名称 (RED, BLUE 等)
                else -> {
                    BukkitChatColor.valueOf(colorStr.uppercase())
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    
    /**
     * 清除玩家的前缀、后缀和颜色
     */
    fun clearPlayer(targetPlayerName: String) {
        val team = playerTeams[targetPlayerName]
        if (team != null) {
            team.removeEntry(targetPlayerName)
            team.unregister()
            playerTeams.remove(targetPlayerName)
        }
    }
    
    /**
     * 获取或创建玩家的 Team
     */
    private fun getOrCreateTeam(targetPlayerName: String): Team {
        var team = playerTeams[targetPlayerName]
        if (team == null) {
            val teamName = "tl_${guiId}_${targetPlayerName.take(10)}_${System.currentTimeMillis() % 10000}"
            team = scoreboard.registerNewTeam(teamName)
            team.addEntry(targetPlayerName)
            playerTeams[targetPlayerName] = team
        }
        return team
    }
    
    /**
     * 动画功能
     */
    fun animate(duration: Int, interval: Int, animation: (TabListGui, Int) -> Unit) {
        animationTask?.cancel()
        val plugin = Bukkit.getPluginManager().getPlugin("Luagin") as? Luagin ?: return
        animationTask = object : BukkitRunnable() {
            var tick = 0
            override fun run() {
                if (tick >= duration) {
                    cancel()
                    return
                }
                animation(this@TabListGui, tick)
                tick += interval
            }
        }
        animationTask?.runTaskTimer(plugin, 0L, interval.toLong())
    }
    
    /**
     * 销毁 TAB 列表，清理所有资源
     */
    fun destroy() {
        // 取消动画任务
        animationTask?.cancel()
        animationTask = null
        
        // 重置玩家的 TAB 列表
        if (isApplied) {
            reset()
        }
        
        // 清理所有 Teams
        playerTeams.values.forEach { team ->
            team.unregister()
        }
        playerTeams.clear()
    }
}

