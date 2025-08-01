package tech.ezeny.luagin

import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luagin.commands.LuaginCommandExecutor
import tech.ezeny.luagin.config.MySQLManager
import tech.ezeny.luagin.di.getKoinModules
import tech.ezeny.luagin.di.pluginModules
import tech.ezeny.luagin.gui.inventory.InventoryGuiListener
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.utils.PLog
import tech.ezeny.luagin.web.WebPanelManager

class Luagin : JavaPlugin() {

    override fun onEnable() {
        // 初始化日志系统
        PLog.initialize(logger)

        // 启动 Koin
        startKoin {
            modules(pluginModules + module { single<Luagin> { this@Luagin } })
        }

        getKoinModules()

        // 注册插件的命令执行器和 Tab 补全器
        val luaginCommandExecutor = LuaginCommandExecutor()
        getCommand("luagin")?.setExecutor(luaginCommandExecutor)
        getCommand("luagin")?.tabCompleter = luaginCommandExecutor

        // 注册 Inventory 事件监听器
        server.pluginManager.registerEvents(InventoryGuiListener, this)

        PLog.info("log.info.loading_completed")
    }

    override fun onDisable() {
        val permissionManager = getKoin().get<PermissionManager>()
        permissionManager.cleanup()

        val mysqlManager = getKoin().get<MySQLManager>()
        mysqlManager.close()

        val webPanelManager = getKoin().get<WebPanelManager>()
        webPanelManager.stopWebServer()

        stopKoin()

        PLog.info("log.info.unloading_completed")
    }
}