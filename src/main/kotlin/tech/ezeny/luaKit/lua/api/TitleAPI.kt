package tech.ezeny.luaKit.lua.api

import org.bukkit.Bukkit
import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luaKit.utils.MessageUtils
import tech.ezeny.luaKit.utils.PLog

object TitleAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        globals.set("print_title", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2) {
                    return NIL
                }

                val title = args.checkjstring(1)
                val subtitle = args.checkjstring(2)

                val playerName = if (args.narg() >= 3 && !args.isnil(3)) {
                    args.checkjstring(3)
                } else {
                    null
                }

                val fadeIn = if (args.narg() >= 4 && args.arg(4).isnumber()) args.checkint(4) else 10
                val stay = if (args.narg() >= 5 && args.arg(5).isnumber()) args.checkint(5) else 70
                val fadeOut = if (args.narg() >= 6 && args.arg(6).isnumber()) args.checkint(6) else 20

                runOnMainThread {
                    if (playerName != null) {
                        // 发送给指定玩家
                        val player = Bukkit.getPlayerExact(playerName)
                        if (player != null) {
                            MessageUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)
                        } else {
                            PLog.warning("log.warning.player_not_found", playerName)
                        }
                    } else {
                        // 发送给所有在线玩家
                        Bukkit.getOnlinePlayers().forEach { player ->
                            MessageUtils.sendTitle(player, title, subtitle, fadeIn, stay, fadeOut)
                        }
                    }
                }

                return NIL
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("print_title")) {
            apiNames.add("print_title")
        }

        PLog.info("log.info.title_api_set")
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}