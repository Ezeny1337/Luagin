package tech.ezeny.luagin.events

import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.PLog
import tech.ezeny.luagin.utils.ScriptUtils

class LuaJITEventHandler(private val lua: Lua, private val functionRef: Int) {
    fun call(event: Any) {
        try {
            lua.rawGetI(LUA_REGISTRYINDEX, functionRef)
            LuaValueFactory.pushJavaObject(lua, event)
            lua.pCall(1, 0)
        } catch (e: Exception) {
            val scriptName = ScriptUtils.getCurrentScript() ?: "unknown"
            PLog.warning("log.warning.handle_event_failed", scriptName, e.message ?: "Unknown error")
        }
    }
}