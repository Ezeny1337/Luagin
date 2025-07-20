package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.performance.PerformanceMonitor
import tech.ezeny.luagin.utils.PLog

object UtilsAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val performanceMonitor: PerformanceMonitor by inject()
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 utils 表
        lua.newTable()

        // clock(): number - 获取程序运行时间（类似 os.clock()）
        lua.push { luaState ->
            val runtime = System.nanoTime() / 1000000.0 // 转换为毫秒
            luaState.push(runtime)
            return@push 1
        }
        lua.setField(-2, "clock")

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

        // get_all_perf(): table - 获取所有性能数据
        lua.push { luaState ->
            val data = performanceMonitor.getAllPerformanceData()
            LuaValueFactory.pushJavaObject(lua, data)
            return@push 1
        }
        lua.setField(-2, "get_all_perf")

        // get_server_perf(): table - 获取服务器性能数据
        lua.push { luaState ->
            val data = performanceMonitor.getPerformanceData("server")
            if (data != null) {
                LuaValueFactory.pushJavaObject(lua, data)
            } else {
                luaState.pushNil()
            }
            return@push 1
        }
        lua.setField(-2, "get_server_perf")

        // get_java_perf(): table - 获取 Java 性能数据
        lua.push { luaState ->
            val data = performanceMonitor.getPerformanceData("java")
            if (data != null) {
                LuaValueFactory.pushJavaObject(lua, data)
            } else {
                luaState.pushNil()
            }
            return@push 1
        }
        lua.setField(-2, "get_java_perf")

        // get_system_perf(): table - 获取系统性能数据
        lua.push { luaState ->
            val data = performanceMonitor.getPerformanceData("system")
            if (data != null) {
                LuaValueFactory.pushJavaObject(lua, data)
            } else {
                luaState.pushNil()
            }
            return@push 1
        }
        lua.setField(-2, "get_system_perf")

        // get_tps(): table - 获取 TPS 信息
        lua.push { luaState ->
            try {
                val tps = performanceMonitor.getServerTPS()

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

        // clear_perf_cache() - 清理性能数据缓存
        lua.push { luaState ->
            performanceMonitor.clearCache()
            return@push 1
        }
        lua.setField(-2, "clear_perf_cache")

        lua.setGlobal("utils")

        if (!apiNames.contains("utils")) {
            apiNames.add("utils")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}
