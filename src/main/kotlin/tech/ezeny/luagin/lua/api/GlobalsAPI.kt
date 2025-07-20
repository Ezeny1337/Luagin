package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.GlobalUtils
import tech.ezeny.luagin.utils.PLog
import java.time.ZoneId
import java.time.ZonedDateTime

object GlobalsAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 globals 表
        lua.newTable()

        // owtime : number - 主世界 Overworld 的相对游戏时间
        val time = GlobalUtils.getOverworldTime()
        lua.push(time.toDouble())
        lua.setField(-2, "owtime")

        // timestamp : number - 当前时间戳
        val timestamp = System.currentTimeMillis().toDouble()
        lua.push(timestamp)
        lua.setField(-2, "timestamp")

        // online_players : number - 当前在线玩家数
        val onlinePlayers = Bukkit.getOnlinePlayers().size
        lua.push(onlinePlayers.toDouble())
        lua.setField(-2, "online_players")

        // max_players : number - 最大玩家数
        val maxPlayers = Bukkit.getServer().maxPlayers
        lua.push(maxPlayers.toDouble())
        lua.setField(-2, "max_players")

        // get_realtime([zoneid: string]): realtime - 获取实时时间
        lua.push { luaState ->
            val zoneStr = if (luaState.top >= 1) luaState.toString(1) else "UTC"

            try {
                val zoneId = ZoneId.of(zoneStr)
                val now = ZonedDateTime.now(zoneId)

                // 返回的 Map
                val result = mapOf(
                    "hour" to now.hour.toDouble(),
                    "minute" to now.minute.toDouble(),
                    "second" to now.second.toDouble()
                )

                LuaValueFactory.pushJavaObject(luaState, result)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.invalid_timezone", zoneStr ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "get_realtime")

        // get_datetime([zoneid: string]): datetime - 获取日期时间
        lua.push { luaState ->
            val zoneStr = if (luaState.top >= 1) luaState.toString(1) else "UTC"

            try {
                val zoneId = ZoneId.of(zoneStr)
                val now = ZonedDateTime.now(zoneId)

                // 返回的 Map
                val result = mapOf(
                    "year" to now.year.toDouble(),
                    "month" to now.monthValue.toDouble(),
                    "day" to now.dayOfMonth.toDouble()
                )

                LuaValueFactory.pushJavaObject(luaState, result)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.invalid_timezone", zoneStr ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "get_datetime")

        // date([format: string, zoneid: string]): string - 格式化日期时间（类似 os.date()）
        lua.push { luaState ->
            val format = if (luaState.top >= 1) luaState.toString(1) ?: "%Y-%m-%d %H:%M:%S" else "%Y-%m-%d %H:%M:%S"
            val zoneStr = if (luaState.top >= 2) luaState.toString(2) ?: "UTC" else "UTC"

            try {
                val formattedDate = GlobalUtils.formatDateTime(format, zoneStr)
                luaState.push(formattedDate)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.date_format_failed", e.message ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "date")

        lua.setGlobal("globals")

        if (!apiNames.contains("globals")) {
            apiNames.add("globals")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 