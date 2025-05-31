package tech.ezeny.luagin.lua.api

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.network.NetworkManager
import tech.ezeny.luagin.utils.PLog

object NetworkAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private lateinit var networkManager: NetworkManager
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
        this.networkManager = NetworkManager(plugin)
    }

    override fun registerAPI(globals: Globals) {
        // 创建 network 表
        val networkTable = LuaTable()
        globals.set("network", networkTable)

        // 注册 GET 请求方法
        networkTable.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val url = args.checkjstring(1)
                val headers = if (args.narg() >= 2 && args.arg(2).istable()) {
                    networkManager.convertLuaTableToMap(args.checktable(2))
                        .mapValues { it.value.toString() }
                } else null
                val callback = if (args.narg() >= 3 && args.arg(3).isfunction()) args.arg(3) else null

                // 如果提供了回调函数，则异步执行
                if (callback != null) {
                    networkManager.doGetRequest(url, headers) { response ->
                        callback.call(valueOf(response))
                    }
                    return NIL
                } else {
                    // 同步执行
                    val response = networkManager.doGetRequest(url, headers)
                    return valueOf(response)
                }
            }
        })

        // 注册 POST 请求方法
        networkTable.set("post", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val url = args.checkjstring(1)
                val data = if (args.narg() >= 2 && args.arg(2).istable()) {
                    networkManager.convertLuaTableToMap(args.checktable(2))
                } else null
                val headers = if (args.narg() >= 3 && args.arg(3).istable()) {
                    networkManager.convertLuaTableToMap(args.checktable(3))
                        .mapValues { it.value.toString() }
                } else null
                val callback = if (args.narg() >= 4 && args.arg(4).isfunction()) args.arg(4) else null

                // 如果提供了回调函数，则异步执行
                if (callback != null) {
                    networkManager.doPostRequest(url, data, headers) { response ->
                        callback.call(valueOf(response))
                    }
                    return NIL
                } else {
                    // 同步执行
                    val response = networkManager.doPostRequest(url, data, headers)
                    return valueOf(response)
                }
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("network")) {
            apiNames.add("network")
        }

        PLog.info("log.info.network_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
}