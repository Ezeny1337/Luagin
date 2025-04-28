package tech.ezeny.luaKit.lua

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.utils.PLog
import tech.ezeny.luaKit.lua.LuaEnvManager.scriptsFolder
import java.io.File

object ScriptManager {

    // 存储每个脚本文件路径及其对应的独立 Lua 环境
    private val scriptEnvironments = mutableMapOf<String, Globals>()

    /**
     * 加载指定目录下的所有Lua脚本
     * @param scriptDir 脚本目录
     * @return 成功加载的脚本数量
     */
    fun loadAllScripts(scriptDir: File): Int {
        if (!scriptDir.exists() || !scriptDir.isDirectory) {
            PLog.warning("log.warning.lua_dir_not_found", scriptDir.canonicalPath)
            return 0
        }

        // 清空现有脚本环境
        scriptEnvironments.clear()

        var loadedCount = 0
        scriptDir.walkTopDown().filter { it.isFile && it.extension == "lua" }.forEach { file ->
            if (loadScript(file)) {
                loadedCount++
            }
        }

        return loadedCount
    }

    /**
     * 加载指定名称的脚本
     * @param scriptFile 脚本文件
     * @return 是否成功加载
     */
    fun loadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists() || !scriptFile.isFile || scriptFile.extension != "lua") {
            PLog.warning("log.warning.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        try {
            // 设置当前正在加载的脚本名称
            EventManager.setCurrentScript(scriptFile.name)

            // 为脚本创建新的独立 Lua 环境
            val scriptGlobals = createScriptEnvironment()

            // 加载并执行脚本
            scriptGlobals.loadfile(scriptFile.absolutePath).call()

            // 存储脚本环境
            scriptEnvironments[scriptFile.absolutePath] = scriptGlobals

            PLog.info("log.info.loading_lua_succeeded", scriptFile.name)
            return true
        } catch (e: Exception) {
            PLog.warning("log.warning.loading_lua_error", scriptFile.name, e.message ?: "Unknown error")
            return false
        }
    }

    /**
     * 重载单个脚本
     * @param scriptFile 脚本文件
     * @return 是否成功重载
     */
    fun reloadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists()) {
            PLog.warning("log.warning.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        // 清理该脚本的事件处理器
        EventManager.clearHandlersForScript(scriptFile.name)

        // 移除旧的脚本环境
        scriptEnvironments.remove(scriptFile.absolutePath)

        // 加载新的脚本
        return loadScript(scriptFile)
    }

    /**
     * 为脚本创建独立的 Lua 环境
     * @return Lua 环境
     */
    private fun createScriptEnvironment(): Globals {
        // 创建基础 Lua 环境
        val globals = JsePlatform.standardGlobals()

        // 从主环境复制共享 API
        copySharedAPIs(globals)

        return globals
    }

    /**
     * 从主环境复制共享 API 到脚本环境
     * @param scriptGlobals 目标脚本环境
     */
    private fun copySharedAPIs(scriptGlobals: Globals) {
        val mainGlobals = LuaEnvManager.globals

        // 获取要共享的 API 列表
        val sharedAPIs = LuaEnvManager.getSharedAPIs()

        for (api in sharedAPIs) {
            val apiValue = mainGlobals.get(api)
            if (!apiValue.isnil()) {
                scriptGlobals.set(api, apiValue)
                PLog.info("log.info.copy_shared_api", api)
            } else {
                PLog.warning("log.info.copy_shared_api_error", api)
            }
        }
    }

    /**
     * 获取脚本名称列表
     * @return 脚本名称列表
     */
    fun getScriptNames(): List<String> {
        val scriptDir = scriptsFolder
        if (!scriptDir.exists() || !scriptDir.isDirectory) {
            return emptyList()
        }

        return scriptDir.listFiles { file ->
            file.isFile && file.extension == "lua"
        }?.map { it.name } ?: emptyList()
    }
}