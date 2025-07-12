package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.commands.CommandManager
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.commands.LuaCommand

object CommandAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val commandManager: CommandManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 cmd 表
        lua.newTable()

        // register 函数 - 注册命令
        lua.push { luaState ->
            if (luaState.top < 3 || !luaState.isString(1) || !luaState.isString(2) || !luaState.isFunction(3)) {
                return@push 0
            }
            val commandName = luaState.toString(1) ?: return@push 0
            val permission = luaState.toString(2) ?: ""

            luaState.pushValue(3)
            val handlerRef = luaState.ref(LUA_REGISTRYINDEX)
            val command = commandManager.registerCommand(commandName, permission, lua, handlerRef)

            // 返回 LuaCommandWrapper
            LuaCommandWrapper.pushToLua(luaState, command)
            return@push 1
        }
        lua.setField(-2, "register")

        // exec 函数 - 执行命令
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }
            val command = luaState.toString(1) ?: return@push 0
            tech.ezeny.luagin.utils.PLog.info("log.info.command_exec", command)
            Bukkit.getScheduler().runTask(plugin, Runnable {
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), command)
                } catch (e: Exception) {
                    tech.ezeny.luagin.utils.PLog.warning(
                        "log.warning.command_exec_failed",
                        command,
                        e.message ?: "Unknown error"
                    )
                }
            })
            return@push 0
        }
        lua.setField(-2, "exec")

        lua.setGlobal("cmd")

        if (!apiNames.contains("cmd")) {
            apiNames.add("cmd")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    /**
     * Lua命令包装器
     */
    class LuaCommandWrapper() {
        companion object {
            fun pushToLua(lua: Lua, command: LuaCommand) {
                lua.newTable()

                // add_args 方法 - 给命令添加参数
                lua.push { luaState ->
                    if (luaState.top < 3 || !luaState.isNumber(2) || !luaState.isTable(3)) {
                        return@push 1
                    }
                    val position = luaState.toInteger(2).toInt()
                    val argsList = mutableListOf<String>()
                    luaState.pushNil()
                    while (luaState.next(3) != 0) {
                        if (luaState.isString(-1)) {
                            argsList.add(luaState.toString(-1) ?: "")
                        }
                        luaState.pop(1)
                    }
                    val permission = if (luaState.top > 3 && luaState.isString(4)) luaState.toString(4) ?: "" else ""
                    command.addArgs(position, argsList, permission)
                    return@push 1
                }
                lua.setField(-2, "add_args")

                // add_args_for 方法 - 给上一级命令添加参数
                lua.push { luaState ->
                    if (luaState.top < 4 || !luaState.isNumber(2) || !luaState.isString(3) || !luaState.isTable(4)) {
                        return@push 1
                    }
                    val position = luaState.toInteger(2).toInt()
                    val previousArg = luaState.toString(3) ?: ""
                    val argsList = mutableListOf<String>()
                    luaState.pushNil()
                    while (luaState.next(4) != 0) {
                        if (luaState.isString(-1)) {
                            argsList.add(luaState.toString(-1) ?: "")
                        }
                        luaState.pop(1)
                    }
                    val permission = if (luaState.top > 4 && luaState.isString(5)) luaState.toString(5) ?: "" else ""
                    command.addArgsForPrevious(position, argsList, previousArg, permission)
                    return@push 1
                }
                lua.setField(-2, "add_args_for")
            }
        }
    }
}