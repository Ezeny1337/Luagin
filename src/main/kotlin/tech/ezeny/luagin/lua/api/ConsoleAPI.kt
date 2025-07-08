package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog

object ConsoleAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        lua.push { luaState ->
            val messages = mutableListOf<String>()

            // 处理所有参数
            for (i in 1..luaState.top) {
                val message = when {
                    luaState.isNil(i) -> {
                        "nil"
                    }
                    luaState.isBoolean(i) -> {
                        if (luaState.toBoolean(i)) "true" else "false"
                    }
                    else -> {
                        luaState.toString(i) ?: "nil"
                    }
                }
                messages.add(message)
            }

            // 输出到控制台
            val fullMessage = messages.joinToString(" ")
            PLog.print(fullMessage)

            return@push 0
        }
        lua.setGlobal("print")

        if (!apiNames.contains("print")) {
            apiNames.add("print")
        }

        PLog.info("log.info.console_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
}