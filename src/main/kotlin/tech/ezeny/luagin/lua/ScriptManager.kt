package tech.ezeny.luagin.lua

import party.iroiro.luajava.Lua
import party.iroiro.luajava.LuaException
import party.iroiro.luajava.luajit.LuaJit
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
    private val scriptEnvironments = mutableMapOf<String, Lua>()

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

        scriptEnvironments.values.forEach { it.close() }
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
        // 清理事件处理器
        eventManager.clearHandlers()
        // 清理共享函数
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
            val scriptLua = createScriptEnvironment()

            scriptLua.run(scriptFile.readText())

            scriptEnvironments[scriptFile.absolutePath] = scriptLua
            ScriptUtils.setCurrentLua(scriptFile.name, scriptLua)

            PLog.info("log.info.loading_lua_succeeded", scriptFile.name)
            true
        } catch (e: Exception) {
            PLog.severe("log.severe.loading_lua_failed", scriptFile.name, e.message ?: "Unknown error")
            false
        } catch (e: LuaException) {
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

        // 清理事件处理器
        eventManager.clearHandlersForScript(scriptFile.name)
        // 清理共享函数和环境
        CommunicationUtils.clearScriptFunctions(scriptFile.name)
        // 清理 ScriptUtils 中的 Lua 实例
        ScriptUtils.removeScriptLua(scriptFile.name)

        scriptEnvironments[scriptFile.absolutePath]?.close()
        scriptEnvironments.remove(scriptFile.absolutePath)

        return loadScript(scriptFile)
    }

    /**
     * 创建一个新的 Lua 环境
     *
     * @return Lua 脚本环境
     */
    private fun createScriptEnvironment(): Lua {
        val lua = LuaJit()
        lua.openLibraries()
        copySharedAPIs(lua)
        return lua
    }

    /**
     * 将共享的 API 注册到新的脚本环境
     *
     * @param scriptLua 新创建的脚本环境
     */
    private fun copySharedAPIs(scriptLua: Lua) {
        // 直接为脚本环境注册所有 API，而不是从主环境复制
        luaEnvManager.apiRegister.registerAllAPIs(scriptLua)
    }

    fun listScripts(): List<String> {
        return scriptsFolder.listFiles { file ->
            file.isFile && file.extension == "lua"
        }?.map { it.name } ?: emptyList()
    }
}