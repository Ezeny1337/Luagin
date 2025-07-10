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

        // owtime 属性（主世界 Overworld 的相对游戏时间）
        val time = GlobalUtils.getOverworldTime()
        lua.push(time.toDouble())
        lua.setField(-2, "owtime")

        // timestamp 属性（当前时间戳）
        val timestamp = System.currentTimeMillis().toDouble()
        lua.push(timestamp)
        lua.setField(-2, "timestamp")

        // get_realtime 函数
        lua.push { luaState ->
            val zoneStr = if (luaState.top >= 2) luaState.toString(2) else "UTC"

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

        // get_datetime 函数
        lua.push { luaState ->
            val zoneStr = if (luaState.top >= 2) luaState.toString(2) else "UTC"

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

        // get_tps 函数
        lua.push { luaState ->
            try {
                val tps = GlobalUtils.getServerTPS()

                // 创建返回的 Map，包含1分钟、5分钟、15分钟的TPS
                val result = mapOf(
                    "tps_1m" to tps[0],
                    "tps_5m" to tps[1],
                    "tps_15m" to tps[2]
                )
                
                LuaValueFactory.pushJavaObject(luaState, result)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.get_tps_failed", e.message ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "get_tps")

        lua.setGlobal("globals")

        // 添加到 API 名称列表
        if (!apiNames.contains("globals")) {
            apiNames.add("globals")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 