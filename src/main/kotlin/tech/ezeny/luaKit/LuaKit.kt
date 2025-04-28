package tech.ezeny.luaKit

import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luaKit.commands.CommandHandler
import tech.ezeny.luaKit.config.YamlManager
import tech.ezeny.luaKit.di.pluginModules
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.i18n.I18n
import tech.ezeny.luaKit.lua.LuaEnvManager
import tech.ezeny.luaKit.lua.ScriptManager
import tech.ezeny.luaKit.utils.PLog

class LuaKit : JavaPlugin() {
    private lateinit var commandHandler: CommandHandler

    override fun onEnable() {
        // 初始化日志系统
        PLog.initialize(logger)

        // 启动 Koin
        startKoin {
            modules(pluginModules + module { single<LuaKit> { this@LuaKit } })
        }

        getKoin().get<YamlManager>()
        getKoin().get<I18n>()
        getKoin().get<EventManager>()
        getKoin().get<LuaEnvManager>()
        getKoin().get<ScriptManager>()

        commandHandler = getKoin().get<CommandHandler>()
        getCommand("luakit")?.setExecutor(commandHandler)
        getCommand("luakit")?.tabCompleter = commandHandler

        PLog.info("log.info.loading_completed")
    }

    override fun onDisable() {
        PLog.info("log.info.unloading_completed")
    }
}