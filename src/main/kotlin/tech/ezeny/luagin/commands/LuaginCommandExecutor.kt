package tech.ezeny.luagin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandExecutor
import org.bukkit.command.CommandSender
import org.bukkit.command.TabCompleter
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.ezeny.luagin.i18n.I18n
import tech.ezeny.luagin.lua.ScriptManager
import tech.ezeny.luagin.utils.PLog
import java.io.File

class LuaginCommandExecutor : CommandExecutor, TabCompleter, KoinComponent {
    private val scriptManager: ScriptManager by inject()
    private val i18n: I18n by inject()

    override fun onCommand(sender: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {
        if (!sender.hasPermission("luagin.admin")) {
            return true
        }

        when {
            args.isEmpty() -> {
                sender.sendMessage(i18n.get("command.check_help"))
            }

            args[0].equals("help", ignoreCase = true) -> {
                sendHelpMessage(sender)
            }

            args[0].equals("reload", ignoreCase = true) -> {
                if (args.size == 1) {
                    sender.sendMessage(i18n.get("command.reload.all.loading"))
                    val count = scriptManager.reloadAllScripts()
                    sender.sendMessage(i18n.get("command.reload.all.success", count))
                } else {
                    val scriptName = args[1]
                    sender.sendMessage(i18n.get("command.reload.script.loading", scriptName))
                    val success = scriptManager.reloadScriptByName(scriptName)
                    if (success) {
                        sender.sendMessage(i18n.get("command.reload.script.success", scriptName))
                    } else {
                        sender.sendMessage(i18n.get("command.reload.script.failure", scriptName))
                    }
                }
            }

            else -> {
                sender.sendMessage(i18n.get("command.check_help"))
            }
        }
        return true
    }

    override fun onTabComplete(
        sender: CommandSender,
        command: Command,
        alias: String,
        args: Array<out String>
    ): MutableList<String> {
        if (!sender.hasPermission("luagin.admin")) {
            return mutableListOf()
        }

        val completions = mutableListOf<String>()
        when (args.size) {
            1 -> {
                if ("reload".startsWith(args[0], ignoreCase = true)) {
                    completions.add("reload")
                }
                if ("help".startsWith(args[0], ignoreCase = true)) {
                    completions.add("help")
                }
            }

            2 -> {
                if (args[0].equals("reload", ignoreCase = true)) {
                    val currentInput = args[1]
                    val scriptNames = scriptManager.listScripts()
                    scriptNames.filter { it.startsWith(currentInput, ignoreCase = true) }
                        .forEach { completions.add(it) }
                }
            }
        }
        return completions
    }

    private fun sendHelpMessage(sender: CommandSender) {
        sender.sendMessage(i18n.get("command.help.title"))
        sender.sendMessage(i18n.get("command.help.reload_all"))
        sender.sendMessage(i18n.get("command.help.reload_specific"))
        sender.sendMessage(i18n.get("command.help.help"))
    }
}