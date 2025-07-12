package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.CommunicationUtils

object CommunicationAPI : LuaAPIProvider, KoinComponent {
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 comm 表
        lua.newTable()

        // expose_func 函数 - 暴露函数
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val functionName = luaState.toString(1) ?: ""
            val handlerIndex = 2

            if (!luaState.isFunction(handlerIndex)) {
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

        // unexpose_func 函数 - 取消暴露函数
        lua.push { luaState ->
            if (luaState.top < 1) {
                return@push 0
            }

            val functionName = luaState.toString(1) ?: ""
            val success = CommunicationUtils.unexposeFunction(functionName)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "unexpose_func")

        // call_func 函数 - 调用暴露的函数
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val scriptName = luaState.toString(1) ?: ""
            val functionName = luaState.toString(2) ?: ""

            // 收集额外参数
            val args = mutableListOf<Any?>()
            for (i in 3..luaState.top) {
                args.add(LuaValueFactory.getLuaValue(luaState, i))
            }

            val result = CommunicationUtils.callFunction(scriptName, functionName, args)
            LuaValueFactory.pushJavaObject(luaState, result)
            return@push 1
        }
        lua.setField(-2, "call_func")

        // get_exposed_functions 函数 - 获取所有暴露函数
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
    }

    override fun getAPINames(): List<String> = apiNames
}