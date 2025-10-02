package tech.ezeny.luagin.gui.chat

import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerChatEvent
import tech.ezeny.luagin.utils.ColorUtils

object ChatListener : Listener {
    
    /**
     * 监听玩家聊天事件，应用前缀和名字颜色
     */
    @EventHandler(priority = EventPriority.HIGH)
    fun onPlayerChat(event: AsyncPlayerChatEvent) {
        val player = event.player
        val playerName = player.name
        val originalMessage = event.message
        
        // 获取玩家的前缀和名字颜色设置
        val prefix = ChatManager.getPlayerPrefix(playerName)
        val nameColor = ChatManager.getPlayerNameColor(playerName)
        val nameGradient = ChatManager.getPlayerNameGradient(playerName)
        
        // 如果玩家有前缀或名字颜色设置，则修改聊天格式
        if (prefix != null || nameColor != null || (nameGradient != null && nameGradient.isNotEmpty())) {
            // 取消原始事件，我们将手动发送格式化的消息
            event.isCancelled = true
            
            // 格式化玩家名字
            val formattedName = when {
                nameGradient != null && nameGradient.isNotEmpty() -> {
                    // 渐变颜色优先
                    when (nameGradient.size) {
                        1 -> ColorUtils.formatString("${nameGradient[0]}$playerName")
                        2 -> ColorUtils.createGradient(playerName, nameGradient[0], nameGradient[1])
                        else -> ColorUtils.createGradient(playerName, nameGradient[0], nameGradient[1], nameGradient[2])
                    }
                }
                nameColor != null -> {
                    // 单色
                    ColorUtils.formatString("$nameColor$playerName")
                }
                else -> playerName
            }
            
            // 构建完整的聊天消息
            val formattedMessage = if (prefix != null) {
                val formattedPrefix = ColorUtils.formatString(prefix)
                "$formattedPrefix $formattedName§r: $originalMessage"
            } else {
                "$formattedName§r: $originalMessage"
            }
            
            // 向所有在线玩家发送格式化的消息
            player.server.onlinePlayers.forEach { recipient ->
                recipient.spigot().sendMessage(ColorUtils.parseColoredText(formattedMessage))
            }
            
            // 向控制台发送消息
            val consoleMessage = if (prefix != null) {
                val cleanPrefix = ColorUtils.formatString(prefix).replace("§[0-9a-fk-orx]".toRegex(), "")
                "$cleanPrefix $playerName: $originalMessage"
            } else {
                "$playerName: $originalMessage"
            }

            val cleanConsoleMessage = consoleMessage.replace("§[0-9a-fk-orx]".toRegex(), "")
            player.server.consoleSender.sendMessage(cleanConsoleMessage)
        }
    }
}
