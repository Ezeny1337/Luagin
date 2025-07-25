package tech.ezeny.luagin.di

import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.commands.CommandManager
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.config.MySQLManager
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.i18n.I18n
import tech.ezeny.luagin.items.ItemManager
import tech.ezeny.luagin.lua.APIRegister
import tech.ezeny.luagin.lua.LuaEnvManager
import tech.ezeny.luagin.lua.ScriptManager
import tech.ezeny.luagin.network.NetworkManager
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.performance.PerformanceMonitor
import tech.ezeny.luagin.web.WebPanelManager

val pluginModules = module {
    single { get<Luagin>() } // 提供 Luagin 实例

    single { YamlManager(get()) }
    single { I18n(get()) }
    single { PerformanceMonitor(get()) }
    single { WebPanelManager(get(), get()) }
    single { NetworkManager(get()) }
    single { PermissionManager(get(), get()) }
    single { CommandManager(get(), get()) }
    single { MySQLManager(get(), get()) }
    single { EventManager(get()) }
    single { ItemManager() }
    single { APIRegister(get()) }
    single { LuaEnvManager(get()) }
    single { ScriptManager(get(), get(), get()) }
}

fun getKoinModules() {
    getKoin().get<YamlManager>()
    getKoin().get<I18n>()
    getKoin().get<PerformanceMonitor>()
    getKoin().get<WebPanelManager>()
    getKoin().get<NetworkManager>()
    getKoin().get<PermissionManager>()
    getKoin().get<CommandManager>()
    getKoin().get<MySQLManager>()
    getKoin().get<EventManager>()
    getKoin().get<ItemManager>()
    getKoin().get<APIRegister>()
    getKoin().get<LuaEnvManager>()
    getKoin().get<ScriptManager>()
}