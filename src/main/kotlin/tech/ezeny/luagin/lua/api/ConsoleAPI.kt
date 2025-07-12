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
        // print 全局函数
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

                    luaState.isNumber(i) -> {
                        val number = luaState.toNumber(i)
                        if (number % 1.0 == 0.0) {
                            number.toLong().toString()
                        } else {
                            number.toString()
                        }
                    }

                    luaState.isString(i) -> {
                        luaState.toString(i) ?: "nil"
                    }

                    // 使用 Lua 的 tostring 函数获取标准格式
                    luaState.isTable(i) -> {
                        luaState.pushValue(i)
                        luaState.getGlobal("tostring")
                        luaState.pushValue(-2)
                        luaState.pCall(1, 1)
                        val result = luaState.toString(-1)
                        luaState.pop(2)
                        result ?: "nil"
                    }

                    luaState.isFunction(i) -> {
                        luaState.pushValue(i)
                        luaState.getGlobal("tostring")
                        luaState.pushValue(-2)
                        luaState.pCall(1, 1)
                        val result = luaState.toString(-1)
                        luaState.pop(2)
                        result ?: "nil"
                    }

                    luaState.isUserdata(i) -> {
                        luaState.pushValue(i)
                        luaState.getGlobal("tostring")
                        luaState.pushValue(-2)
                        luaState.pCall(1, 1)
                        val result = luaState.toString(-1)
                        luaState.pop(2)
                        result ?: "nil"
                    }

                    luaState.isThread(i) -> {
                        luaState.pushValue(i)
                        luaState.getGlobal("tostring")
                        luaState.pushValue(-2)
                        luaState.pCall(1, 1)
                        val result = luaState.toString(-1)
                        luaState.pop(2)
                        result ?: "nil"
                    }

                    else -> {
                        luaState.toString(i) ?: "unknown"
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
    }

    override fun getAPINames(): List<String> = apiNames
}