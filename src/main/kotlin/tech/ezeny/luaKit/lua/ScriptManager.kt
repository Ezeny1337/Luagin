package tech.ezeny.luaKit.lua

import org.luaj.vm2.Globals
import org.luaj.vm2.lib.jse.JsePlatform
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.utils.PLog
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

    fun loadAllScripts(): Int {
        if (!scriptsFolder.exists() || !scriptsFolder.isDirectory) {
            PLog.warning("log.warning.lua_dir_not_found", scriptsFolder.canonicalPath)
            return 0
        }

        scriptEnvironments.clear()
        var loadedCount = 0

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
        return loadAllScripts()
    }

    fun loadScriptByName(scriptName: String): Boolean {
        val scriptFile = File(scriptsFolder, scriptName)
        return loadScript(scriptFile)
    }

    private fun loadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists() || !scriptFile.isFile || scriptFile.extension != "lua") {
            PLog.warning("log.warning.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        return try {
            eventManager.setCurrentScript(scriptFile.name)
            val scriptGlobals = createScriptEnvironment()
            scriptGlobals.loadfile(scriptFile.absolutePath).call()
            scriptEnvironments[scriptFile.absolutePath] = scriptGlobals
            PLog.info("log.info.loading_lua_succeeded", scriptFile.name)
            true
        } catch (e: Exception) {
            PLog.warning("log.warning.loading_lua_error", scriptFile.name, e.message ?: "Unknown error")
            false
        }
    }

    fun reloadScriptByName(scriptName: String): Boolean {
        val scriptFile = File(scriptsFolder, scriptName)
        return reloadScript(scriptFile)
    }

    private fun reloadScript(scriptFile: File): Boolean {
        if (!scriptFile.exists()) {
            PLog.warning("log.warning.lua_not_found", scriptFile.canonicalPath)
            return false
        }

        eventManager.clearHandlersForScript(scriptFile.name)
        scriptEnvironments.remove(scriptFile.absolutePath)
        return loadScript(scriptFile)
    }

    private fun createScriptEnvironment(): Globals {
        val globals = JsePlatform.standardGlobals()
        copySharedAPIs(globals)
        return globals
    }

    private fun copySharedAPIs(scriptGlobals: Globals) {
        val mainGlobals = luaEnvManager.globals
        val sharedAPIs = luaEnvManager.getSharedAPIs()
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

    fun getLoadedScriptNames(): List<String> {
        return scriptsFolder.listFiles { file ->
            file.isFile && file.extension == "lua"
        }?.map { it.name } ?: emptyList()
    }
}
