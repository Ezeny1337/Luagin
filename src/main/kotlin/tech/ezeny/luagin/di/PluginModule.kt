package tech.ezeny.luagin.di

import org.koin.dsl.module
import org.koin.java.KoinJavaComponent.getKoin
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.config.MySQLManager
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.i18n.I18n
import tech.ezeny.luagin.lua.APIRegister
import tech.ezeny.luagin.lua.LuaEnvManager
import tech.ezeny.luagin.lua.ScriptManager

val pluginModules = module {
    single { get<Luagin>() } // 提供 Luagin 实例

    single { YamlManager(get()) }
    single { I18n(get()) }
    single { MySQLManager(get(), get()) }
    single { EventManager(get()) }
    single { APIRegister(get()) }
    single { LuaEnvManager(get(), get()) }
    single { ScriptManager(get(), get()) }
}

fun getKoinModules() {
    getKoin().get<YamlManager>()
    getKoin().get<I18n>()
    getKoin().get<MySQLManager>()
    getKoin().get<EventManager>()
    getKoin().get<APIRegister>()
    getKoin().get<LuaEnvManager>()
    getKoin().get<ScriptManager>()
}