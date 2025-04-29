package tech.ezeny.luagin.utils

import net.md_5.bungee.api.chat.TextComponent
import org.bukkit.entity.Player

object MessageUtils {
    
    /**
     * 向玩家发送带颜色的私人消息
     * @param player 接收消息的玩家
     * @param message 包含颜色代码的消息
     */
    fun sendColoredMessage(player: Player, message: String) {
        player.spigot().sendMessage(ColorUtils.parseColoredText(message))
    }
    
    /**
     * 向玩家发送带颜色的标题消息
     * @param player 接收消息的玩家
     * @param title 标题文本
     * @param subtitle 副标题文本
     * @param fadeIn 淡入时间（tick）
     * @param stay 停留时间（tick）
     * @param fadeOut 淡出时间（tick）
     */
    fun sendTitle(player: Player, title: String, subtitle: String, fadeIn: Int = 15, stay: Int = 70, fadeOut: Int = 15) {
        val formattedTitle = ColorUtils.formatString(title)
        val formattedSubtitle = ColorUtils.formatString(subtitle)
        player.sendTitle(formattedTitle, formattedSubtitle, fadeIn, stay, fadeOut)
    }
    
    /**
     * 向玩家发送带颜色的动作栏消息
     * @param player 接收消息的玩家
     * @param message 包含颜色代码的消息
     */
    fun sendActionBar(player: Player, message: String) {
        val formattedMessage = ColorUtils.formatString(message)
        player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, TextComponent(formattedMessage))
    }
}