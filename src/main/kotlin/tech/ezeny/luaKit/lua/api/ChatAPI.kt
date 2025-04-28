package tech.ezeny.luaKit.lua.api

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luaKit.utils.MessageUtils
import tech.ezeny.luaKit.utils.PLog

object ChatAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        globals.set("print_chat", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1) {
                    return NIL
                }

                val message = args.checkjstring(1)
                val playerName = if (args.narg() >= 2 && !args.isnil(2)) {
                    args.checkjstring(2)
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

                return NIL
            }
        })

        if (!apiNames.contains("print_chat")) {
            apiNames.add("print_chat")
        }

        PLog.info("log.info.chat_api_set")
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}