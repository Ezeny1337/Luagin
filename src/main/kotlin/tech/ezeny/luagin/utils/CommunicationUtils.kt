package tech.ezeny.luagin.utils

import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.lua.LuaValueFactory
import java.util.concurrent.ConcurrentHashMap

object CommunicationUtils {
    // 存储暴露的函数引用
    private val exposedFunctions = ConcurrentHashMap<String, MutableMap<String, Int>>()
    // 存储 Lua 实例引用
    private val luaInstances = ConcurrentHashMap<String, Lua>()

    /**
     * 暴露函数
     *
     * @param functionName 函数名
     * @param functionRef 函数引用
     * @return 是否成功
     */
    fun exposeFunction(functionName: String, functionRef: Int): Boolean {
        val scriptName = ScriptUtils.getCurrentScript() ?: return false
        val lua = ScriptUtils.getCurrentLua() ?: return false

        if (scriptName.isEmpty()) return false

        // 获取当前脚本的函数映射，如果不存在则创建
        val scriptFunctions = exposedFunctions.computeIfAbsent(scriptName) {
            ConcurrentHashMap<String, Int>()
        }

        // 存储函数引用和 Lua 实例
        scriptFunctions[functionName] = functionRef
        luaInstances[scriptName] = lua
        
        return true
    }

    /**
     * 取消暴露函数
     *
     * @param functionName 函数名
     * @return 是否成功
     */
    fun unexposeFunction(functionName: String): Boolean {
        val scriptName = ScriptUtils.getCurrentScript() ?: return false

        if (scriptName.isEmpty()) return false

        // 获取当前脚本的函数映射
        val scriptFunctions = exposedFunctions[scriptName]
        if (scriptFunctions != null && scriptFunctions.containsKey(functionName)) {
            val functionRef = scriptFunctions.remove(functionName)
            // 释放函数引用
            val lua = luaInstances[scriptName]
            if (lua != null && functionRef != null) {
                lua.unref(functionRef)
            }
            return true
        }

        return false
    }

    /**
     * 调用函数
     *
     * @param scriptName 目标脚本名
     * @param functionName 函数名
     * @param args 参数
     * @return 函数返回值
     */
    fun callFunction(scriptName: String, functionName: String, args: List<Any?>): Any? {
        // 获取目标脚本的函数映射
        val scriptFunctions = exposedFunctions[scriptName]
        if (scriptFunctions == null) {
            PLog.warning("log.warning.script_not_found", scriptName)
            return null
        }

        // 获取目标函数引用
        val functionRef = scriptFunctions[functionName]
        if (functionRef == null) {
            PLog.warning("log.warning.function_not_found", scriptName, functionName)
            return null
        }

        // 获取对应的Lua实例
        val lua = luaInstances[scriptName]
        if (lua == null) {
            PLog.warning("log.warning.lua_instance_not_found", scriptName)
            return null
        }

        // 调用函数并返回结果
        return try {
            lua.rawGetI(LUA_REGISTRYINDEX, functionRef)
            
            args.forEach { arg ->
                LuaValueFactory.pushJavaObject(lua, arg)
            }
            
            lua.pCall(args.size, 1)
            
            // 获取返回值
            val result = LuaValueFactory.getLuaValue(lua, -1)
            
            lua.pop(1)
            result
        } catch (e: Exception) {
            PLog.warning(
                "log.warning.function_call_failed",
                scriptName,
                functionName,
                e.message ?: "Unknown error"
            )
            null
        }
    }

    /**
     * 清除指定脚本的所有暴露函数
     *
     * @param scriptName 脚本名
     */
    fun clearScriptFunctions(scriptName: String) {
        val scriptFunctions = exposedFunctions.remove(scriptName)
        val lua = luaInstances.remove(scriptName)

        // 释放所有函数引用
        if (scriptFunctions != null && lua != null) {
            scriptFunctions.values.forEach { functionRef ->
                lua.unref(functionRef)
            }
        }
    }

    /**
     * 清除所有脚本的暴露函数
     */
    fun clearAllFunctions() {
        exposedFunctions.forEach { (scriptName, functions) ->
            val lua = luaInstances[scriptName]
            if (lua != null) {
                functions.values.forEach { functionRef ->
                    lua.unref(functionRef)
                }
            }
        }
        exposedFunctions.clear()
        luaInstances.clear()
    }
}