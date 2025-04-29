package tech.ezeny.luagin.utils

import org.luaj.vm2.LuaFunction
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import java.util.concurrent.ConcurrentHashMap

object CommunicationUtils {
    // 存储暴露的函数
    private val exposedFunctions = ConcurrentHashMap<String, MutableMap<String, LuaFunction>>()

    /**
     * 暴露函数
     * @param functionName 函数名
     * @param function 函数对象
     * @return 是否成功
     */
    fun exposeFunction(functionName: String, function: LuaFunction): Boolean {
        val scriptName = ScriptUtils.getCurrentScript() ?: return false

        if (scriptName.isEmpty()) return false

        // 获取当前脚本的函数映射，如果不存在则创建
        val scriptFunctions = exposedFunctions.computeIfAbsent(scriptName) {
            ConcurrentHashMap<String, LuaFunction>()
        }

        // 存储函数
        scriptFunctions[functionName] = function
        return true
    }

    /**
     * 取消暴露函数
     * @param functionName 函数名
     * @return 是否成功
     */
    fun unexposeFunction(functionName: String): Boolean {
        val scriptName = ScriptUtils.getCurrentScript() ?: return false

        if (scriptName.isEmpty()) return false

        // 获取当前脚本的函数映射
        val scriptFunctions = exposedFunctions[scriptName]
        if (scriptFunctions != null && scriptFunctions.containsKey(functionName)) {
            scriptFunctions.remove(functionName)
            return true
        }

        return false
    }

    /**
     * 调用函数
     * @param scriptName 目标脚本名
     * @param functionName 函数名
     * @param args 参数
     * @return 函数返回值
     */
    fun callFunction(scriptName: String, functionName: String, args: Varargs): Varargs {
        // 获取目标脚本的函数映射
        val scriptFunctions = exposedFunctions[scriptName]
        if (scriptFunctions == null) {
            PLog.warning("log.warning.script_not_found", scriptName)
            return LuaValue.NIL
        }

        // 获取目标函数
        val function = scriptFunctions[functionName]
        if (function == null) {
            PLog.warning("log.warning.function_not_found", scriptName, functionName)
            return LuaValue.NIL
        }

        // 调用函数并返回结果
        return try {
            function.invoke(args)
        } catch (e: Exception) {
            PLog.warning(
                "log.warning.function_call_error",
                scriptName,
                functionName,
                e.message ?: "Unknown error"
            )
            LuaValue.NIL
        }
    }

    /**
     * 获取所有暴露的函数
     * @return 包含所有暴露函数信息的Lua表
     */
    fun getExposedFunctions(): LuaTable {
        val result = LuaTable()
        var index = 1

        exposedFunctions.forEach { (scriptName, functions) ->
            val scriptTable = LuaTable()
            scriptTable.set("script", LuaValue.valueOf(scriptName))

            val functionsTable = LuaTable()
            var funcIndex = 1
            functions.keys.forEach { functionName ->
                functionsTable.set(funcIndex++, LuaValue.valueOf(functionName))
            }

            scriptTable.set("functions", functionsTable)
            result.set(index++, scriptTable)
        }

        return result
    }

    /**
     * 清除指定脚本的所有暴露函数
     * @param scriptName 脚本名
     */
    fun clearScriptFunctions(scriptName: String) {
        exposedFunctions.remove(scriptName)
    }

    /**
     * 清除所有脚本的暴露函数
     */
    fun clearAllFunctions() {
        exposedFunctions.clear()
    }
}