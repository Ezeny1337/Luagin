package tech.ezeny.luagin.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJit
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.io.File

class LuaEnvManager(plugin: Luagin, val apiRegister: APIRegister) {

    lateinit var lua: Lua
        private set
    var scriptsFolder: File = File(plugin.dataFolder, "scripts")

    init {
        ensureScriptsFolderExists()

        setEnvironment()
    }

    fun setEnvironment() {
        lua = LuaJit()
        lua.openLibraries()

        setupBasicEnvironment()
        PLog.info("log.info.set_environment")
    }

    /**
     * 设置基础 Lua 环境
     * 注册所有 API
     */
    private fun setupBasicEnvironment() {
        apiRegister.registerAllAPIs(lua)
    }

    private fun ensureScriptsFolderExists() {
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
        }
    }

    /**
     * 获取已注册的 API 列表
     */
    fun getSharedAPIs(): List<String> = apiRegister.apiNames

    /**
     * 关闭 Lua 环境
     */
    fun close() {
        lua.close()
    }
}