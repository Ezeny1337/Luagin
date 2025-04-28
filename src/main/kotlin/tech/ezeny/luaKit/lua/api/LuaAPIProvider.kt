package tech.ezeny.luaKit.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals

interface LuaAPIProvider {
    fun initialize(plugin: JavaPlugin) {}
    fun registerAPI(globals: Globals)
    fun getAPINames(): List<String>
}