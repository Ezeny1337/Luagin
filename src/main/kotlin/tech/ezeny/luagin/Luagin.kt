package tech.ezeny.luagin

import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luagin.commands.CommandHandler
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.di.pluginModules
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.i18n.I18n
import tech.ezeny.luagin.lua.APIRegister
import tech.ezeny.luagin.lua.LuaEnvManager
import tech.ezeny.luagin.lua.ScriptManager
import tech.ezeny.luagin.utils.PLog

class Luagin : JavaPlugin() {
    private lateinit var commandHandler: CommandHandler

    override fun onEnable() {
        // 初始化日志系统
        PLog.initialize(logger)

        // 启动 Koin
        startKoin {
            modules(pluginModules + module { single<Luagin> { this@Luagin } })
        }

        getKoin().get<YamlManager>()
        getKoin().get<I18n>()
        getKoin().get<EventManager>()
        getKoin().get<APIRegister>()
        getKoin().get<LuaEnvManager>()
        getKoin().get<ScriptManager>()

        commandHandler = getKoin().get<CommandHandler>()
        getCommand("luagin")?.setExecutor(commandHandler)
        getCommand("luagin")?.tabCompleter = commandHandler

        PLog.info("log.info.loading_completed")
    }

    override fun onDisable() {
        PLog.info("log.info.unloading_completed")
    }
}