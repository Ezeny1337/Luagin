package tech.ezeny.luagin.lua.api

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

        // get_tps(): table - 获取服务器 tps
        lua.push { luaState ->
            try {
                val tps = GlobalUtils.getServerTPS()

                luaState.newTable()
                luaState.push(tps[0])
                luaState.rawSetI(-2, 1)
                luaState.push(tps[1])
                luaState.rawSetI(-2, 2)
                luaState.push(tps[2])
                luaState.rawSetI(-2, 3)

                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.get_tps_failed", e.message ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "get_tps")

        lua.setGlobal("globals")

        if (!apiNames.contains("globals")) {
            apiNames.add("globals")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 