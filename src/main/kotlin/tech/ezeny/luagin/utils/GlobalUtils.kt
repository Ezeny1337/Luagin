package tech.ezeny.luagin.utils

import org.bukkit.Bukkit
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object GlobalUtils {
    
    /**
     * 获取主世界时间
     *
     * @return 主世界的当前时间（单位为 tick，每天 24000 tick）
     */
    fun getOverworldTime(): Long {
        return try {
            val overworld = Bukkit.getWorlds().find { it.environment == org.bukkit.World.Environment.NORMAL }
            overworld?.time ?: 0L
        } catch (e: Exception) {
            PLog.warning("log.warning.get_overworld_time_failed", e.message ?: "Unknown error")
            0L
        }
    }

    /**
     * 格式化日期时间（类似 os.date()）
     *
     * @param format 格式化字符串，支持以下格式：
     *   %Y - 四位年份 (2024)
     *   %y - 两位年份 (24)
     *   %m - 月份 (01-12)
     *   %d - 日期 (01-31)
     *   %H - 24小时制小时 (00-23)
     *   %h - 12小时制小时 (01-12)
     *   %M - 分钟 (00-59)
     *   %S - 秒 (00-59)
     *   %A - 星期全名 (Monday)
     *   %a - 星期缩写 (Mon)
     *   %B - 月份全名 (January)
     *   %b - 月份缩写 (Jan)
     *   %p - AM/PM
     *   %z - 时区偏移
     *   %Z - 时区名称
     *   %c - 标准日期时间格式
     *   %x - 标准日期格式
     *   %X - 标准时间格式
     *   %j - 一年中的第几天
     *   %U - 一年中的第几周（周日开始）
     *   %W - 一年中的第几周（周一开始）
     *   %V - ISO 8601 周数
     *   %G - ISO 8601 年份
     *   %g - ISO 8601 年份（2位）
     *   %u - 星期几（1-7，周一是1）
     *   %w - 星期几（0-6，周日是0）
     *   %% - 百分号
     * @param zoneId 时区ID，默认为系统默认时区
     * @return 格式化后的日期时间字符串
     */
    fun formatDateTime(format: String, zoneId: String = "UTC"): String {
        return try {
            val zone = ZoneId.of(zoneId)
            val now = ZonedDateTime.now(zone)
            
            // 将 Lua 风格的格式转换为 Java 的 DateTimeFormatter 格式
            val javaFormat = convertLuaFormatToJava(format)
            val formatter = DateTimeFormatter.ofPattern(javaFormat, Locale.ENGLISH)
            
            now.format(formatter)
        } catch (e: Exception) {
            PLog.warning("log.warning.format_datetime_failed", e.message ?: "Unknown error")
            "Error: Invalid format or timezone"
        }
    }

    /**
     * 将 Lua 风格的日期格式转换为 Java 的 DateTimeFormatter 格式
     */
    private fun convertLuaFormatToJava(luaFormat: String): String {
        return luaFormat
            .replace("%Y", "yyyy")
            .replace("%y", "yy")
            .replace("%m", "MM")
            .replace("%d", "dd")
            .replace("%H", "HH")
            .replace("%h", "hh")
            .replace("%M", "mm")
            .replace("%S", "ss")
            .replace("%A", "EEEE")
            .replace("%a", "EEE")
            .replace("%B", "MMMM")
            .replace("%b", "MMM")
            .replace("%p", "a")
            .replace("%z", "Z")
            .replace("%Z", "z")
            .replace("%c", "EEE MMM dd HH:mm:ss yyyy")
            .replace("%x", "MM/dd/yy")
            .replace("%X", "HH:mm:ss")
            .replace("%j", "D")
            .replace("%U", "w")
            .replace("%W", "w")
            .replace("%V", "ww")
            .replace("%G", "yyyy")
            .replace("%g", "yy")
            .replace("%u", "e")
            .replace("%w", "c")
            .replace("%%", "%")
    }
} 