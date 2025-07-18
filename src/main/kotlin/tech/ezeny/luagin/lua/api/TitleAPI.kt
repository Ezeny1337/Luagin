package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.MessageUtils
import tech.ezeny.luagin.utils.PLog

object TitleAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
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

        if (!apiNames.contains("print_title")) {
            apiNames.add("print_title")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}