package tech.ezeny.luagin.utils

import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.TextComponent

object ColorUtils {

    // Hex 颜色的正则表达式模式，匹配 \a 后跟 6 位十六进制字符
    val HEX_PATTERN = "\u0007([0-9a-fA-F]{6})".toRegex()

    /**
     * 将带有颜色代码的字符串转换为 TextComponent
     * 支持 \aRRGGBB 格式的 Hex 颜色和 § 格式的原版颜色代码
     *
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
     *
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

    /**
     * 创建双色渐变文本
     * 
     * @param text 渐变文本
     * @param startColor 起始颜色 Hex
     * @param endColor 结束颜色 Hex
     * @return 渐变后的文本
     */
    fun createGradient(text: String, startColor: String, endColor: String): String {
        if (text.isEmpty()) return text
        
        try {
            val start = ChatColor.of(startColor)
            val end = ChatColor.of(endColor)
            
            val result = StringBuilder()
            val length = text.length
            
            for (i in text.indices) {
                val ratio = i.toDouble() / (length - 1)
                val interpolatedColor = interpolateColor(start, end, ratio)
                result.append(interpolatedColor).append(text[i])
            }
            
            return result.toString()
        } catch (e: Exception) {
            PLog.warning("log.warning.invalid_gradient_color", startColor, endColor)
            return text
        }
    }

    /**
     * 创建三色渐变文本
     * 
     * @param text 渐变文本
     * @param startColor 起始颜色 Hex
     * @param middleColor 中间颜色 Hex
     * @param endColor 结束颜色 Hex
     * @return 渐变后的文本
     */
    fun createGradient(text: String, startColor: String, middleColor: String, endColor: String): String {
        if (text.isEmpty()) return text
        
        try {
            val start = ChatColor.of(startColor)
            val middle = ChatColor.of(middleColor)
            val end = ChatColor.of(endColor)
            
            val result = StringBuilder()
            val length = text.length
            
            for (i in text.indices) {
                val ratio = i.toDouble() / (length - 1)
                val interpolatedColor = if (ratio <= 0.5) {
                    val firstHalfRatio = ratio * 2
                    interpolateColor(start, middle, firstHalfRatio)
                } else {
                    val secondHalfRatio = (ratio - 0.5) * 2
                    interpolateColor(middle, end, secondHalfRatio)
                }
                result.append(interpolatedColor).append(text[i])
            }
            
            return result.toString()
        } catch (e: Exception) {
            PLog.warning("log.warning.invalid_gradient_color", startColor, middleColor, endColor)
            return text
        }
    }

    /**
     * 插值两个颜色
     */
    private fun interpolateColor(color1: ChatColor, color2: ChatColor, ratio: Double): String {
        try {
            // 获取颜色的 RGB 值
            val rgb1 = color1.color?.rgb ?: 0xFFFFFF
            val rgb2 = color2.color?.rgb ?: 0xFFFFFF
            
            // 插值 RGB 值
            val r1 = (rgb1 shr 16) and 0xFF
            val g1 = (rgb1 shr 8) and 0xFF
            val b1 = rgb1 and 0xFF
            
            val r2 = (rgb2 shr 16) and 0xFF
            val g2 = (rgb2 shr 8) and 0xFF
            val b2 = rgb2 and 0xFF
            
            val r = (r1 + (r2 - r1) * ratio).toInt().coerceIn(0, 255)
            val g = (g1 + (g2 - g1) * ratio).toInt().coerceIn(0, 255)
            val b = (b1 + (b2 - b1) * ratio).toInt().coerceIn(0, 255)
            
            // 转换为 Hex 格式
            val hexColor = String.format("#%02X%02X%02X", r, g, b)
            return ChatColor.of(hexColor).toString()
        } catch (e: Exception) {
            // 如果插值失败，返回第一个颜色
            return color1.toString()
        }
    }
}