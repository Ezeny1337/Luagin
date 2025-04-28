package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.utils.PLog

object UtilsAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        // 创建 utils 表
        val utilsTable = LuaTable()
        globals.set("utils", utilsTable)

        // 注册 command_exec 函数
        utilsTable.set("command_exec", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val command = args.checkjstring(1)

                runOnMainThread {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    } catch (e: Exception) {
                        PLog.warning("log.warning.command_exec_error", command, e.message ?: "Unknown error")
                    }
                }

                return NIL
            }
        })

        // 注册 execute_after 函数
        utilsTable.set("execute_after", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isnumber() || !args.arg(2).isfunction()) {
                    return NIL
                }

                val delaySeconds = args.checkdouble(1)
                val delayTicks = (delaySeconds * 20).toLong()  // 将秒转换为 ticks
                val callback = args.checkfunction(2)

                // 收集额外参数
                val extraArgs = if (args.narg() > 2) {
                    val varargs = varargsOf(Array(args.narg() - 2) { i -> args.arg(i + 3) })
                    varargs
                } else {
                    NONE
                }

                // 使用 Bukkit 调度器延迟执行
                Bukkit.getScheduler().runTaskLater(plugin, Runnable {
                    try {
                        callback.invoke(extraArgs)
                    } catch (e: Exception) {
                        PLog.warning("log.warning.execute_after_error", e.message ?: "Unknown error")
                    }
                }, delayTicks)

                return NIL
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("utils")) {
            apiNames.add("utils")
        }

        PLog.info("log.info.utils_api_set")
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}
