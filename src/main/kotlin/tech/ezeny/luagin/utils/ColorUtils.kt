package tech.ezeny.luagin.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent

object ColorUtils {

    // Hex 颜色的正则表达式模式，匹配 \a 后跟 6 位十六进制字符
    val HEX_PATTERN = "\\\\a([0-9a-fA-F]{6})".toRegex()

    /**
     * 将带有颜色代码的字符串转换为 TextComponent
     * 支持 \aRRGGBB 格式的 Hex 颜色和 § 格式的原版颜色代码
     * @param text 包含颜色代码的文本
     * @return 格式化后的 TextComponent
     */
    fun parseColoredText(text: String): TextComponent {
        // 简化处理方式，先将文本转换为标准格式
        val formattedText = formatString(text)

        // 使用 Bukkit 的 ChatColor 处理所有颜色代码
        val result = TextComponent()

        // 跟踪当前文本格式状态
        var currentColor: ChatColor? = null
        var isBold = false
        var isItalic = false
        var isUnderlined = false
        var isStrikethrough = false
        var isObfuscated = false

        var currentText = ""
        var i = 0

        while (i < formattedText.length) {
            if (formattedText[i] == '§' && i + 1 < formattedText.length) {
                // 如果有累积的文本，先添加到结果中
                if (currentText.isNotEmpty()) {
                    val component = TextComponent(currentText)
                    if (currentColor != null) component.color = currentColor
                    component.isBold = isBold
                    component.isItalic = isItalic
                    component.isUnderlined = isUnderlined
                    component.isStrikethrough = isStrikethrough
                    component.isObfuscated = isObfuscated
                    result.addExtra(component)
                    currentText = ""
                }

                // 处理颜色代码
                val code = formattedText[i + 1]
                if (code == 'x' && i + 13 < formattedText.length) {
                    // 处理 Hex 颜色代码 (§x§r§r§g§g§b§b 格式)
                    if (formattedText[i + 2] == '§' && formattedText[i + 4] == '§' &&
                        formattedText[i + 6] == '§' && formattedText[i + 8] == '§' &&
                        formattedText[i + 10] == '§' && formattedText[i + 12] == '§'
                    ) {

                        val hexColor = "#" + formattedText[i + 3] + formattedText[i + 5] +
                                formattedText[i + 7] + formattedText[i + 9] +
                                formattedText[i + 11] + formattedText[i + 13]

                        try {
                            currentColor = ChatColor.of(hexColor)
                        } catch (e: Exception) {
                            PLog.warning("log.warning.invalid_color", hexColor)
                        }

                        i += 14 // 跳过整个 Hex 颜色代码
                        continue
                    }
                } else {
                    // 处理普通颜色代码
                    when (code) {
                        '0' -> {
                            currentColor = ChatColor.BLACK
                        }

                        '1' -> {
                            currentColor = ChatColor.DARK_BLUE
                        }

                        '2' -> {
                            currentColor = ChatColor.DARK_GREEN
                        }

                        '3' -> {
                            currentColor = ChatColor.DARK_AQUA
                        }

                        '4' -> {
                            currentColor = ChatColor.DARK_RED
                        }

                        '5' -> {
                            currentColor = ChatColor.DARK_PURPLE
                        }

                        '6' -> {
                            currentColor = ChatColor.GOLD
                        }

                        '7' -> {
                            currentColor = ChatColor.GRAY
                        }

                        '8' -> {
                            currentColor = ChatColor.DARK_GRAY
                        }

                        '9' -> {
                            currentColor = ChatColor.BLUE
                        }

                        'a' -> {
                            currentColor = ChatColor.GREEN
                        }

                        'b' -> {
                            currentColor = ChatColor.AQUA
                        }

                        'c' -> {
                            currentColor = ChatColor.RED
                        }

                        'd' -> {
                            currentColor = ChatColor.LIGHT_PURPLE
                        }

                        'e' -> {
                            currentColor = ChatColor.YELLOW
                        }

                        'f' -> {
                            currentColor = ChatColor.WHITE
                        }

                        'k' -> {
                            isObfuscated = true
                        }

                        'l' -> {
                            isBold = true
                        }

                        'm' -> {
                            isStrikethrough = true
                        }

                        'n' -> {
                            isUnderlined = true
                        }

                        'o' -> {
                            isItalic = true
                        }

                        'r' -> {
                            currentColor = ChatColor.WHITE
                            isBold = false
                            isItalic = false
                            isUnderlined = false
                            isStrikethrough = false
                            isObfuscated = false
                        }
                    }

                    i += 2 // 跳过颜色代码
                    continue
                }
            }

            // 添加普通字符
            currentText += formattedText[i]
            i++
        }

        // 添加最后一段文本
        if (currentText.isNotEmpty()) {
            val component = TextComponent(currentText)
            if (currentColor != null) component.color = currentColor
            component.isBold = isBold
            component.isItalic = isItalic
            component.isUnderlined = isUnderlined
            component.isStrikethrough = isStrikethrough
            component.isObfuscated = isObfuscated
            result.addExtra(component)
        }

        return result
    }

    /**
     * 格式化字符串，将颜色代码转换为可读格式
     * 支持 \aRRGGBB 格式的 Hex 颜色和 § 格式的原版颜色代码
     * @param text 包含颜色代码的文本
     * @return 处理后的文本
     */
    fun formatString(text: String): String {
        // 处理 Hex 颜色代码
        return text.replace(HEX_PATTERN) { matchResult ->
            val hexColor = matchResult.groupValues[1]
            "§x§${hexColor[0]}§${hexColor[1]}§${hexColor[2]}§${hexColor[3]}§${hexColor[4]}§${hexColor[5]}"
        }
    }
}