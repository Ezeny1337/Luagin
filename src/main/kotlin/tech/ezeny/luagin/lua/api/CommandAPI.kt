package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.commands.CommandManager
import tech.ezeny.luagin.commands.LuaCommand
import tech.ezeny.luagin.utils.PLog

object CommandAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val commandManager: CommandManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        // 创建 command 表
        val commandTable = LuaTable()
        globals.set("command", commandTable)

        // 注册命令
        commandTable.set("register", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 3 || !args.arg(1).isstring() || !args.arg(2).isstring() || !args.arg(3)
                        .isfunction()
                ) {
                    return NIL
                }

                val commandName = args.checkjstring(1)
                val permission = args.checkjstring(2)
                val handler = args.checkfunction(3)

                val command = commandManager.registerCommand(commandName, permission, handler)

                // 创建 Lua 命令包装器
                val luaCommand = LuaCommandWrapper(command)
                return luaCommand
            }
        })

        // 注册 exec 函数
        commandTable.set("exec", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val command = args.checkjstring(1)

                runOnMainThread {
                    try {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                    } catch (e: Exception) {
                        PLog.warning("log.warning.command_exec_failed", command, e.message ?: "Unknown error")
                    }
                }

                return NIL
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("command")) {
            apiNames.add("command")
        }

        PLog.info("log.info.command_api_set")
    }

    override fun getAPINames(): List<String> = apiNames

    /**
     * Lua命令包装器
     */
    class LuaCommandWrapper(private val command: LuaCommand) : LuaTable() {
        init {
            // 添加命令参数提示（不指定前置参数）
            set("add_args", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    // 第一个参数是 self
                    if (args.narg() < 3 || !args.arg(2).isnumber() || !args.arg(3).istable()) {
                        return this@LuaCommandWrapper
                    }

                    val position = args.checkint(2)
                    val argsTable = args.checktable(3)
                    val argsList = mutableListOf<String>()

                    // 解析参数列表
                    var i = 1
                    while (true) {
                        val arg = argsTable.get(i)
                        if (arg.isnil()) break
                        if (arg.isstring()) {
                            argsList.add(arg.tojstring())
                        }
                        i++
                    }

                    // 检查是否提供了权限参数
                    val permission = if (args.narg() > 3 && args.arg(4).isstring()) {
                        args.checkjstring(4)
                    } else {
                        ""  // 默认为空，表示使用上级权限
                    }

                    command.addArgs(position, argsList, permission)
                    return this@LuaCommandWrapper
                }
            })

            // 添加命令参数提示（指定前置参数）
            set("add_args_for", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    // 第一个参数是 self
                    if (args.narg() < 4 || !args.arg(2).isnumber() || !args.arg(3).isstring() || !args.arg(4)
                            .istable()
                    ) {
                        return this@LuaCommandWrapper
                    }

                    val position = args.checkint(2)
                    val previousArg = args.checkjstring(3)
                    val argsTable = args.checktable(4)
                    val argsList = mutableListOf<String>()

                    // 解析参数列表
                    var i = 1
                    while (true) {
                        val arg = argsTable.get(i)
                        if (arg.isnil()) break
                        if (arg.isstring()) {
                            argsList.add(arg.tojstring())
                        }
                        i++
                    }

                    // 检查是否提供了权限参数
                    val permission = if (args.narg() > 4 && args.arg(5).isstring()) {
                        args.checkjstring(5)
                    } else {
                        ""  // 默认为空，表示使用上级权限
                    }

                    command.addArgsForPrevious(position, argsList, previousArg, permission)
                    return this@LuaCommandWrapper
                }
            })
        }
    }

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}