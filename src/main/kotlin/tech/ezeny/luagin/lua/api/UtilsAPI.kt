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

        // run_timer(delay: number, period: number, callback: function[, ...]): number - 设置重复执行的定时器
        lua.push { luaState ->
            if (luaState.top < 3) {
                return@push 0
            }

            val delaySeconds = luaState.toNumber(1)
            val periodSeconds = luaState.toNumber(2)
            val delayTicks = (delaySeconds * 20).toLong() // 将秒转换为 ticks
            val periodTicks = (periodSeconds * 20).toLong()
            val callbackIndex = 3

            if (!luaState.isFunction(callbackIndex)) {
                return@push 0
            }

            // 将回调函数和额外参数存储到注册表中
            luaState.pushValue(callbackIndex)
            val callbackRef = luaState.ref()

            // 收集额外参数
            val extraArgs = mutableListOf<Any?>()
            for (i in 4..luaState.top) {
                extraArgs.add(luaState.toJavaObject(i))
            }

            // 使用 Bukkit 调度器重复执行
            val taskId = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
                try {
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        // 推送额外参数
                        extraArgs.forEach { arg ->
                            LuaValueFactory.pushJavaObject(lua, arg)
                        }
                        luaState.pCall(extraArgs.size, 0)
                    }
                } catch (e: Exception) {
                    PLog.warning("log.warning.run_timer_failed", e.message ?: "Unknown error")
                    // 如果发生异常，释放回调引用
                    luaState.unref(callbackRef)
                }
            }, delayTicks, periodTicks).taskId

            luaState.push(taskId.toLong())
            return@push 1
        }
        lua.setField(-2, "run_timer")

        // run_timer_once(delay: number, callback: function[, ...]): number - 设置单次执行的定时器
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val delaySeconds = luaState.toNumber(1)
            val delayTicks = (delaySeconds * 20).toLong()
            val callbackIndex = 2

            if (!luaState.isFunction(callbackIndex)) {
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
            val taskId = Bukkit.getScheduler().runTaskLater(plugin, Runnable {
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
                    PLog.warning("log.warning.run_timer_once_failed", e.message ?: "Unknown error")
                }
            }, delayTicks).taskId

            luaState.push(taskId.toLong())
            return@push 1
        }
        lua.setField(-2, "run_timer_once")

        // cancel_timer(taskid: number): boolean - 取消计时器
        lua.push { luaState ->
            if (luaState.top < 1) {
                return@push 0
            }

            val taskId = luaState.toInteger(1).toInt()
            
            try {
                val scheduler = Bukkit.getScheduler()
                
                // 尝试取消任务
                scheduler.cancelTask(taskId)
                
                // 由于cancelTask没有返回值，我们假设取消成功
                // 如果任务不存在，cancelTask也不会抛出异常
                luaState.push(true)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.cancel_timer_failed", e.message ?: "Unknown error")
                luaState.push(false)
                return@push 1
            }
        }
        lua.setField(-2, "cancel_timer")

        lua.setGlobal("utils")

        if (!apiNames.contains("utils")) {
            apiNames.add("utils")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}
