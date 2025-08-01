package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.MessageUtils
import tech.ezeny.luagin.utils.PLog

object MessageAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // print(text: string[, ...]) - 输出内容到控制台
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

        // print_chat(text: string[, player_name: string]) - 输出内容到聊天
        lua.push { luaState ->
            if (luaState.top < 1) {
                return@push 0
            }

            val message = luaState.toString(1) ?: ""
            val playerName = if (luaState.top >= 2 && !luaState.isNil(2)) {
                luaState.toString(2)
            } else {
                null
            }

            runOnMainThread {
                if (playerName != null) {
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player != null) {
                        MessageUtils.sendColoredMessage(player, message)
                    } else {
                        PLog.warning("log.warning.player_not_found", playerName)
                    }
                } else {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        MessageUtils.sendColoredMessage(player, message)
                    }
                }
            }

            return@push 0
        }
        lua.setGlobal("print_chat")

        // print_title(title: string, sub_title: string[, player_name: string, fade_in:number, stay:number, fade_out:number]) - 输出内容到标题位置
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val title = luaState.toString(1) ?: ""
            val subtitle = luaState.toString(2) ?: ""

            val playerName = if (luaState.top >= 3 && !luaState.isNil(3)) {
                luaState.toString(3)
            } else {
                null
            }

            val fadeIn = if (luaState.top >= 4 && luaState.isNumber(4)) luaState.toInteger(4).toInt() else 10
            val stay = if (luaState.top >= 5 && luaState.isNumber(5)) luaState.toInteger(5).toInt() else 70
            val fadeOut = if (luaState.top >= 6 && luaState.isNumber(6)) luaState.toInteger(6).toInt() else 20

            runOnMainThread {
                if (playerName != null) {
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player != null) {
                        MessageUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)
                    } else {
                        PLog.warning("log.warning.player_not_found", playerName)
                    }
                } else {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        MessageUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)
                    }
                }
            }

            return@push 0
        }
        lua.setGlobal("print_title")

        // print_actionbar(text: string[, player_name: string]) - 输出内容到 ActionBar 位置
        lua.push { luaState ->
            if (luaState.top < 1) {
                return@push 0
            }

            val message = luaState.toString(1) ?: ""
            val playerName = if (luaState.top >= 2 && !luaState.isNil(2)) {
                luaState.toString(2)
            } else {
                null
            }

            runOnMainThread {
                if (playerName != null) {
                    val player = Bukkit.getPlayerExact(playerName)
                    if (player != null) {
                        MessageUtils.sendActionBar(player, message)
                    } else {
                        PLog.warning("log.warning.player_not_found", playerName)
                    }
                } else {
                    Bukkit.getOnlinePlayers().forEach { player ->
                        MessageUtils.sendActionBar(player, message)
                    }
                }
            }

            return@push 0
        }
        lua.setGlobal("print_actionbar")

        // 添加所有API名称到列表
        val allApiNames = listOf("print", "print_chat", "print_title", "print_actionbar")
        allApiNames.forEach { name ->
            if (!apiNames.contains(name)) {
                apiNames.add(name)
            }
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
} 