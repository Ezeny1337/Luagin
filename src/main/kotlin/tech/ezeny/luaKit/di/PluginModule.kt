package tech.ezeny.luaKit.di

import org.koin.dsl.module
import tech.ezeny.luaKit.LuaKit
import tech.ezeny.luaKit.commands.CommandHandler
import tech.ezeny.luaKit.config.YamlManager
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.i18n.I18n
import tech.ezeny.luaKit.lua.LuaEnvManager
import tech.ezeny.luaKit.lua.ScriptManager

val pluginModules = module {
    single { get<LuaKit>() } // 提供 LuaKit 实例

    single { YamlManager(get()) }
    single { I18n(get()) }
    single { EventManager(get()) }
    single { LuaEnvManager(get()) }
    single { ScriptManager(get(), get()) }
    single { CommandHandler(get(),get()) }
}