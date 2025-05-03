package tech.ezeny.luagin.lua.api

import org.luaj.vm2.Globals
import tech.ezeny.luagin.Luagin

interface LuaAPIProvider {
    fun initialize(plugin: Luagin) {}
    fun registerAPI(globals: Globals)
    fun getAPINames(): List<String>
}