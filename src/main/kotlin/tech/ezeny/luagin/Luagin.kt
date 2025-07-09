package tech.ezeny.luagin

import org.bukkit.plugin.java.JavaPlugin
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luagin.commands.LuaginCommandExecutor
import tech.ezeny.luagin.di.getKoinModules
import tech.ezeny.luagin.di.pluginModules
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.utils.FileUtils
import tech.ezeny.luagin.utils.PLog

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

        PLog.info("log.info.loading_completed")
    }

    override fun onDisable() {
        val permissionManager = getKoin().get<PermissionManager>()
        permissionManager.cleanup()
        stopKoin()

        PLog.info("log.info.unloading_completed")
    }
}