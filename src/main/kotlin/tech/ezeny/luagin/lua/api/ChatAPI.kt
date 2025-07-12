package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.MessageUtils
import tech.ezeny.luagin.utils.PLog

object ChatAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // print_chat 全局函数 - 输出内容到聊天
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

        if (!apiNames.contains("print_chat")) {
            apiNames.add("print_chat")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}