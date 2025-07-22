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
        lua.newTable() // globals

        // timestamp : number - 当前时间戳
        lua.push(System.currentTimeMillis().toDouble())
        lua.setField(-2, "timestamp")

        // online_players : table - 玩家名字列表
        val playerNames = GlobalUtils.getOverworldPlayerNames()
        lua.newTable()
        playerNames.forEachIndexed { idx, name ->
            lua.push(name)
            lua.rawSetI(-2, idx + 1)
        }
        lua.setField(-2, "online_players")

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

        // 设置元表
        lua.newTable()

        // __index
        lua.push { luaState ->
            val key = luaState.toString(2)
            when (key) {
                // owtime : number - 主世界 Overworld 的相对游戏时间
                "owtime" -> {
                    val time = GlobalUtils.getOverworldTime()
                    luaState.push(time.toDouble())
                    return@push 1
                }
                //owweather : string - 主世界的天气
                "owweather" -> {
                    val weather = GlobalUtils.getOverworldWeather()
                    luaState.push(weather)
                    return@push 1
                }
                else -> {
                    luaState.getField(1, key)
                    return@push 1
                }
            }
        }
        lua.setField(-2, "__index")

        // __newindex
        lua.push { luaState ->
            val key = luaState.toString(2)
            when (key) {
                "owtime" -> {
                    val newTime = luaState.toInteger(3)
                    GlobalUtils.setOverworldTime(newTime.toLong())
                    return@push 0
                }
                "owweather" -> {
                    val newWeather = luaState.toString(3)
                    if (newWeather != null) {
                        GlobalUtils.setOverworldWeather(newWeather)
                    }
                    return@push 0
                }
                else -> {
                    luaState.setField(1, key)
                    return@push 0
                }
            }
        }
        lua.setField(-2, "__newindex")

        // 绑定元表
        lua.setMetatable(-2)
        lua.setGlobal("globals")

        if (!apiNames.contains("globals")) {
            apiNames.add("globals")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 