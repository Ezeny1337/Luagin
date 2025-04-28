package tech.ezeny.luagin.lua

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.io.File

class LuaEnvManager(plugin: Luagin) {

    lateinit var globals: Globals
        private set
    var scriptsFolder: File = File(plugin.dataFolder, "scripts")

    /**
     * 获取已注册的 API 列表
     */
    fun getSharedAPIs(): List<String> = APIRegister.apiNames

    init {
        ensureScriptsFolderExists()

        // 初始化 API 系统
        APIRegister.initialize(plugin)

        setEnvironment()
    }

    fun setEnvironment() {
        globals = JsePlatform.standardGlobals()
        setupBasicEnvironment()
        PLog.info("log.info.set_environment")
    }

    /**
     * 设置基础 Lua 环境
     * 注册所有 API
     */
    private fun setupBasicEnvironment() {
        APIRegister.registerAllAPIs(globals)
    }

    private fun ensureScriptsFolderExists() {
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
        }
    }
}