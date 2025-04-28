package tech.ezeny.luagin.di

import org.koin.dsl.module
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.commands.CommandHandler
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.i18n.I18n
import tech.ezeny.luagin.lua.LuaEnvManager
import tech.ezeny.luagin.lua.ScriptManager

val pluginModules = module {
    single { get<Luagin>() } // 提供 Luagin 实例

    single { YamlManager(get()) }
    single { I18n(get()) }
    single { EventManager(get()) }
    single { LuaEnvManager(get()) }
    single { ScriptManager(get(), get()) }
    single { CommandHandler(get(),get()) }
}