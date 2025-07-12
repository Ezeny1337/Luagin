package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.network.NetworkManager

object NetworkAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private lateinit var networkManager: NetworkManager
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
        this.networkManager = NetworkManager(plugin)
    }

    override fun registerAPI(lua: Lua) {
        // 创建 network 表
        lua.newTable()

        // get 函数 - 建立 GET 请求
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val url = luaState.toString(1) ?: ""
            val headers = if (luaState.top >= 2 && luaState.isTable(2)) {
                networkManager.convertLuaTableToMap(luaState.toJavaObject(2) as? Map<*, *>)
                    .mapValues { it.value.toString() }
            } else null

            val callbackIndex = if (luaState.top >= 3 && luaState.isFunction(3)) 3 else -1

            // 如果提供了回调函数，则异步执行
            if (callbackIndex != -1) {
                luaState.pushValue(callbackIndex)
                val callbackRef = luaState.ref()

                networkManager.doGetRequest(url, headers) { response ->
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        luaState.push(response)
                        luaState.pCall(1, 0)
                    }
                    luaState.unref(callbackRef)
                }
                luaState.pushNil()
                return@push 1
            } else {
                // 同步执行
                val response = networkManager.doGetRequest(url, headers)
                if (response == null) {
                    luaState.pushNil()
                    return@push 1
                }

                luaState.push(response)
                return@push 1
            }
        }
        lua.setField(-2, "get")

        // post 函数 - 建立 POST 请求
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val url = luaState.toString(1) ?: ""
            val data = if (luaState.top >= 2 && luaState.isTable(2)) {
                networkManager.convertLuaTableToMap(luaState.toJavaObject(2) as? Map<*, *>)
            } else null
            val headers = if (luaState.top >= 3 && luaState.isTable(3)) {
                networkManager.convertLuaTableToMap(luaState.toJavaObject(3) as? Map<*, *>)
                    .mapValues { it.value.toString() }
            } else null

            val callbackIndex = if (luaState.top >= 4 && luaState.isFunction(4)) 4 else -1

            // 如果提供了回调函数，则异步执行
            if (callbackIndex != -1) {
                luaState.pushValue(callbackIndex)
                val callbackRef = luaState.ref()

                networkManager.doPostRequest(url, data, headers) { response ->
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        luaState.push(response)
                        luaState.pCall(1, 0)
                    }
                    luaState.unref(callbackRef)
                }
                luaState.pushNil()
                return@push 1
            } else {
                // 同步执行
                val response = networkManager.doPostRequest(url, data, headers)
                if (response == null) {
                    luaState.pushNil()
                    return@push 1
                }

                luaState.push(response)
                return@push 1
            }
        }
        lua.setField(-2, "post")

        lua.setGlobal("network")

        if (!apiNames.contains("network")) {
            apiNames.add("network")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}