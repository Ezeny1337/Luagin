package tech.ezeny.luagin.lua

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.utils.CommunicationUtils
import tech.ezeny.luagin.utils.PLog
import tech.ezeny.luagin.utils.ScriptUtils
import java.io.File

class ScriptManager(
    private val eventManager: EventManager,
    private val luaEnvManager: LuaEnvManager
) {
    private val scriptsFolder: File = luaEnvManager.scriptsFolder
    private val scriptEnvironments = mutableMapOf<String, Globals>()

    init {
        ensureScriptsFolderExists()
        loadAllScripts()
    }

    private fun ensureScriptsFolderExists() {
        if (!scriptsFolder.exists()) {
            scriptsFolder.mkdirs()
        }
    }

    /**
     * 加载目录中的所有 Lua 脚本
     *
     * @return 成功加载的脚本数量
     */
    fun loadAllScripts(): Int {
        if (!scriptsFolder.exists() || !scriptsFolder.isDirectory) {
            PLog.severe("log.severe.lua_dir_not_found", scriptsFolder.canonicalPath)
            return 0
        }

        scriptEnvironments.clear()
        var loadedCount = 0

        // 遍历脚本目录并加载所有 Lua 脚本
        scriptsFolder.walkTopDown()
            .filter { it.isFile && it.extension == "lua" }
            .forEach { file ->
                if (loadScript(file)) loadedCount++
            }

        PLog.info("log.info.lua_loading_completed", loadedCount)
        return loadedCount
    }

    fun reloadAllScripts(): Int {
        eventManager.clearHandlers()
        CommunicationUtils.clearAllFunctions()
        return loadAllScripts()
    }

    /**
     * 加载单个 Lua 脚本
     *
     * @param scriptFile 要加载的 Lua 脚本文件
     * @return 是否成功加载该脚本
     */
    private fun loadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists() || !scriptFile.isFile || scriptFile.extension != "lua") {
            PLog.severe("log.severe.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        return try {
            ScriptUtils.setCurrentScript(scriptFile.name)
            val scriptGlobals = createScriptEnvironment()
            scriptGlobals.loadfile(scriptFile.absolutePath).call()
            scriptEnvironments[scriptFile.absolutePath] = scriptGlobals
            PLog.info("log.info.loading_lua_succeeded", scriptFile.name)
            true
        } catch (e: Exception) {
            PLog.severe("log.severe.loading_lua_failed", scriptFile.name, e.message ?: "Unknown error")
            false
        }
    }

    fun reloadScriptByName(scriptName: String): Boolean {
        val scriptFile = File(scriptsFolder, scriptName)
        return reloadScript(scriptFile)
    }

    /**
     * 重新加载指定的 Lua 脚本文件
     *
     * @param scriptFile Lua 脚本文件
     * @return 是否成功重新加载该脚本
     */
    private fun reloadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists()) {
            PLog.severe("log.severe.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        // 清理之前的事件处理器
        eventManager.clearHandlersForScript(scriptFile.name)
        // 清理共享函数和环境
        CommunicationUtils.clearScriptFunctions(scriptFile.name)
        scriptEnvironments.remove(scriptFile.absolutePath)
        return loadScript(scriptFile)
    }

    /**
     * 创建一个新的 Lua 环境
     *
     * @return Lua 脚本环境
     */
    private fun createScriptEnvironment(): Globals {
        val globals = JsePlatform.standardGlobals()
        copySharedAPIs(globals)
        return globals
    }

    /**
     * 将共享的 API 从主环境复制到新的脚本环境
     *
     * @param scriptGlobals 新创建的脚本环境
     */
    private fun copySharedAPIs(scriptGlobals: Globals) {
        val mainGlobals = luaEnvManager.globals
        val sharedAPIs = luaEnvManager.getSharedAPIs()
        for (api in sharedAPIs) {
            val apiValue = mainGlobals.get(api)
            if (!apiValue.isnil()) {
                scriptGlobals.set(api, apiValue)
            } else {
                PLog.severe("log.severe.copy_shared_api_failed", api)
            }
        }
    }

    fun listScripts(): List<String> {
        return scriptsFolder.listFiles { file ->
            file.isFile && file.extension == "lua"
        }?.map { it.name } ?: emptyList()
    }
}
