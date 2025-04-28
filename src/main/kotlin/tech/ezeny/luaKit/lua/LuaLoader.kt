package tech.ezeny.luaKit.lua

import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.utils.PLog
import tech.ezeny.luaKit.lua.LuaEnvManager.scriptsFolder
import java.io.File
import kotlin.collections.isEmpty

object LuaLoader {
    /**
     * 加载所有脚本
     * @return 成功加载的脚本数量
     */
    fun loadScripts(): Int {
        val scriptFiles = scriptsFolder.listFiles { _, name -> name.endsWith(".lua") }
        if (scriptFiles == null || scriptFiles.isEmpty()) {
            PLog.warning("log.warning.scripts_not_found", scriptsFolder.canonicalPath)
            return 0
        }

        // 使用 ScriptManager 加载所有脚本
        val loadedCount = ScriptManager.loadAllScripts(scriptsFolder)

        PLog.info("log.info.lua_loading_completed", loadedCount)
        return loadedCount
    }

    /**
     * 加载指定名称的脚本
     * @param scriptName 脚本名称
     * @return 是否成功加载
     */
    fun loadScript(scriptName: String): Boolean {
        val scriptFile = File(scriptsFolder, scriptName)
        if (!scriptFile.exists() || !scriptFile.isFile) {
            PLog.warning("log.warning.script_not_found", scriptFile.canonicalPath)
            return false
        }

        // 使用 ScriptManager 重载单个脚本
        // 相当于加载
        return ScriptManager.reloadScript(scriptFile)
    }

    /**
     * 重载所有脚本
     * @return 成功重载的脚本数量
     */
    fun reloadScripts(): Int {
        // 清理现有事件处理器
        EventManager.clearHandlers()

        // 重置 Lua 主环境
        LuaEnvManager.setEnvironment()

        // 重新加载所有脚本
        val loadedCount = loadScripts()

        return loadedCount
    }
}