package tech.ezeny.luaKit.lua

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import tech.ezeny.luaKit.lua.api.APIRegister
import tech.ezeny.luaKit.utils.PLog
import java.io.File

object LuaEnvManager {

    lateinit var globals: Globals
        private set
    lateinit var scriptsFolder: File
        private set

    private lateinit var plugin: JavaPlugin

    // 获取当前已注册的共享 API 列表
    fun getSharedAPIs(): List<String> = APIRegister.apiNames

    fun initialize(plugin: JavaPlugin){
        this.plugin = plugin
        scriptsFolder = File(plugin.dataFolder, "scripts")
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

    private fun setupBasicEnvironment() {
        // 注册所有 API
        APIRegister.registerAllAPIs(globals)
    }

    private fun ensureScriptsFolderExists() {
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
        }
    }
}