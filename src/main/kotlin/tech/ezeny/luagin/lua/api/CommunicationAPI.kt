package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.utils.CommunicationUtils
import tech.ezeny.luagin.utils.PLog

object CommunicationAPI : LuaAPIProvider, KoinComponent {
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 comm 表
        lua.newTable()

        // 暴露函数
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.error("Usage: comm.expose_func(functionName, function)")
                return@push 0
            }

            val functionName = luaState.toString(1) ?: ""
            val handlerIndex = 2

            if (!luaState.isFunction(handlerIndex)) {
                luaState.error("Second argument must be a function")
                return@push 0
            }

            // 将 Lua 函数引用存储到注册表中
            luaState.pushValue(handlerIndex)
            val functionRef = luaState.ref()

            val success = CommunicationUtils.exposeFunction(functionName, functionRef)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "expose_func")

        // 取消暴露函数
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.error("Usage: comm.unexpose_func(functionName)")
                return@push 0
            }

            val functionName = luaState.toString(1) ?: ""
            val success = CommunicationUtils.unexposeFunction(functionName)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "unexpose_func")

        // 调用其他脚本函数
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.error("Usage: comm.call_func(scriptName, functionName[, ...])")
                return@push 0
            }

            val scriptName = luaState.toString(1) ?: ""
            val functionName = luaState.toString(2) ?: ""

            // 收集额外参数
            val args = mutableListOf<Any?>()
            for (i in 3..luaState.top) {
                args.add(luaState.toJavaObject(i))
            }

            val result = CommunicationUtils.callFunction(scriptName, functionName, args)
            when (result) {
                null -> luaState.pushNil()
                is String -> luaState.push(result)
                is Boolean -> luaState.push(result)
                is Int -> luaState.push(result.toLong())
                is Long -> luaState.push(result)
                is Float -> luaState.push(result.toDouble())
                is Double -> luaState.push(result)
                is Number -> luaState.push(result)
                is Collection<*> -> luaState.push(result)
                is Map<*, *> -> luaState.push(result)
                else -> luaState.pushJavaObject(result)
            }

            return@push 1
        }
        lua.setField(-2, "call_func")

        // 获取所有暴露函数
        lua.push { luaState ->
            val exposedFunctions = CommunicationUtils.getExposedFunctions()
            luaState.push(exposedFunctions)
            return@push 1
        }
        lua.setField(-2, "get_exposed_functions")

        lua.setGlobal("comm")

        if (!apiNames.contains("comm")) {
            apiNames.add("comm")
        }

        PLog.info("log.info.comm_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
}