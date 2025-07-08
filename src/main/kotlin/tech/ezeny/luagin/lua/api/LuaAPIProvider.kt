package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin

interface LuaAPIProvider {
    fun initialize(plugin: Luagin) {}
    fun registerAPI(lua: Lua)
    fun getAPINames(): List<String>
}