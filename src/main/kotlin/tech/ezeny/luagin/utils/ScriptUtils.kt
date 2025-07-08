package tech.ezeny.luagin.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.events.EventManager
import kotlin.getValue

object ScriptUtils : KoinComponent {
    private val eventManager: EventManager by inject()

    // 存储当前脚本对应的 Lua 实例
    private val scriptLuaInstances = mutableMapOf<String, Lua>()

    // 获取当前正在加载的脚本名称
    fun getCurrentScript(): String? {
        return eventManager.getCurrentScript()
    }

    // 设置当前正在加载的脚本名称
    fun setCurrentScript(scriptName: String) {
        eventManager.setCurrentScript(scriptName)
    }

    // 获取当前脚本的 Lua 实例
    fun getCurrentLua(): Lua? {
        val scriptName = getCurrentScript() ?: return null
        return scriptLuaInstances[scriptName]
    }

    // 设置当前脚本的 Lua 实例
    fun setCurrentLua(scriptName: String, lua: Lua) {
        scriptLuaInstances[scriptName] = lua
    }

    // 移除脚本的 Lua 实例
    fun removeScriptLua(scriptName: String) {
        scriptLuaInstances.remove(scriptName)
    }
}