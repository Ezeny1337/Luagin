package tech.ezeny.luaKit.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import tech.ezeny.luaKit.i18n.I18n
import tech.ezeny.luaKit.lua.LuaLoader
import tech.ezeny.luaKit.lua.ScriptManager

class CommandHandler : CommandExecutor, TabCompleter {

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (command.name.equals("luakit", ignoreCase = true)) {
            if (args.isEmpty()) {
                sendHelp(sender)
                return true
            }

            when (args[0].lowercase()) {
                "reload" -> {
                    if (args.size > 1) {
                        // 重载特定脚本
                        val scriptName = if (args[1].endsWith(".lua")) args[1] else "${args[1]}.lua"
                        sender.sendMessage("§6${I18n.get("command.reload.script.loading", scriptName)}")
                        val result = LuaLoader.loadScript(scriptName)
                        if (result) {
                            sender.sendMessage("§a${I18n.get("command.reload.script.success", scriptName)}")
                        } else {
                            sender.sendMessage("§c${I18n.get("command.reload.script.failure", scriptName)}")
                        }
                    } else {
                        // 重载所有脚本
                        sender.sendMessage("§6${I18n.get("command.reload.all.loading")}")
                        val loadedCount = LuaLoader.reloadScripts()
                        sender.sendMessage("§a${I18n.get("command.reload.all.success", loadedCount)}")
                    }
                    return true
                }
                "help" -> {
                    sendHelp(sender)
                    return true
                }
                else -> {
                    sender.sendMessage("§c${I18n.get("command.unknown")}")
                    return true
                }
            }
        }
        return false
    }

    private fun sendHelp(sender: CommandSender) {
        sender.sendMessage("§6${I18n.get("command.help.title")}")
        sender.sendMessage("§f${I18n.get("command.help.reload_all")}")
        sender.sendMessage("§f${I18n.get("command.help.reload_specific")}")
        sender.sendMessage("§f${I18n.get("command.help.help")}")
    }

    override fun onTabComplete(sender: CommandSender, command: Command, alias: String, args: Array<out String>): List<String>? {
        if (command.name.equals("luakit", ignoreCase = true)) {
            if (args.size == 1) {
                // 主命令补全
                return listOf("reload", "help").filter { it.startsWith(args[0].lowercase()) }
            } else if (args.size == 2 && args[0].equals("reload", ignoreCase = true)) {
                // 脚本名称补全
                return ScriptManager.getScriptNames().filter {
                    it.startsWith(args[1].lowercase()) 
                }
            }
        }
        return emptyList()
    }
}