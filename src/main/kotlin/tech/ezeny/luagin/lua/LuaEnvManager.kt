package tech.ezeny.luagin.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJit
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog

class LuaEnvManager(plugin: Luagin, val apiRegister: APIRegister) {

    init {
        PLog.info("log.info.api_register_initialized")
    }

    /**
     * 创建一个新的 Lua 环境
     *
     * @return Lua 脚本环境
     */
    fun createScriptEnvironment(): Lua {
        val lua = LuaJit()
        lua.openLibraries()

        // 禁用库
        lua.pushNil(); lua.setGlobal("ffi")
        lua.pushNil(); lua.setGlobal("io")
        lua.pushNil(); lua.setGlobal("os")

        copySharedAPIs(lua)
        return lua
    }

    /**
     * 将共享的 API 注册到新的脚本环境
     *
     * @param scriptLua 新创建的脚本环境
     */
    private fun copySharedAPIs(scriptLua: Lua) {
        // 直接为脚本环境注册所有 API
        apiRegister.registerAllAPIs(scriptLua)
    }
}