package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.PLog

object UtilsAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 utils 表
        lua.newTable()

        // 注册 execute_after 函数
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.error("Usage: utils.execute_after(delaySeconds, callback[, ...])")
                return@push 0
            }

            val delaySeconds = luaState.toNumber(1)
            val delayTicks = (delaySeconds * 20).toLong()  // 将秒转换为 ticks
            val callbackIndex = 2

            if (!luaState.isFunction(callbackIndex)) {
                luaState.error("Second argument must be a function")
                return@push 0
            }

            // 将回调函数和额外参数存储到注册表中
            luaState.pushValue(callbackIndex)
            val callbackRef = luaState.ref()

            // 收集额外参数
            val extraArgs = mutableListOf<Any?>()
            for (i in 3..luaState.top) {
                extraArgs.add(luaState.toJavaObject(i))
            }

            // 使用 Bukkit 调度器延迟执行
            Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                try {
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        // 推送额外参数
                        extraArgs.forEach { arg ->
                            LuaValueFactory.pushJavaObject(lua, arg)
                        }
                        luaState.pCall(extraArgs.size, 0)
                    }
                    luaState.unref(callbackRef)
                } catch (e: Exception) {
                    PLog.warning("log.warning.execute_after_failed", e.message ?: "Unknown error")
                }
            }, delayTicks)

            return@push 0
        }
        lua.setField(-2, "execute_after")

        lua.setGlobal("utils")

        // 添加到 API 名称列表
        if (!apiNames.contains("utils")) {
            apiNames.add("utils")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}
