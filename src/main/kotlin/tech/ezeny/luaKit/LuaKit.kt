package tech.ezeny.luaKit

import org.bukkit.plugin.java.JavaPlugin
import tech.ezeny.luaKit.commands.CommandHandler
import tech.ezeny.luaKit.config.YamlManager
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.i18n.I18n
import tech.ezeny.luaKit.utils.PLog
import tech.ezeny.luaKit.lua.LuaEnvManager
import tech.ezeny.luaKit.lua.LuaLoader

class LuaKit : JavaPlugin() {

    private lateinit var commandHandler: CommandHandler

    override fun onEnable() {
        // 初始化日志系统
        PLog.initialize(logger)

        PLog.info(I18n.get("log.info.loading"))

        // 初始化 YAML 管理器
        YamlManager.initialize(this)

        // 初始化 I18n 系统
        I18n.initialize(this)

        // 初始化事件管理器
        EventManager.initialize(this)

        // 初始化 Lua 环境
        LuaEnvManager.initialize(this)

        // 加载所有 Lua 脚本
        LuaLoader.loadScripts()

        // 注册命令
        commandHandler = CommandHandler()
        getCommand("luakit")?.setExecutor(commandHandler)
        getCommand("luakit")?.tabCompleter = commandHandler

        PLog.info("log.info.loading_completed")
    }

    override fun onDisable() {
        PLog.info("log.info.unloading")

        // 清理事件处理器
        EventManager.clearHandlers()

        PLog.info("log.info.unloading_completed")
    }
}
