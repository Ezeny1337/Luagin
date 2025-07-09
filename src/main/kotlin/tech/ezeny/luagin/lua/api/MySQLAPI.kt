package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.config.MySQLManager
import tech.ezeny.luagin.utils.PLog

object MySQLAPI : LuaAPIProvider, KoinComponent {
    private val mysqlManager: MySQLManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 mysql 表
        lua.newTable()

        // 创建表
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val tableName = luaState.toString(1) ?: ""
            if (!luaState.isTable(2)) {
                luaState.push(false)
                return@push 1
            }

            val columns = mutableMapOf<String, String>()

            // 遍历Lua表获取列定义
            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val key = luaState.toString(-2)
                val value = luaState.toString(-1)
                if (key != null && value != null) {
                    columns[key] = value
                }
                luaState.pop(1)
            }

            try {
                mysqlManager.createTable(tableName, columns)
                luaState.push(true)
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.mysql_create_table_failed", tableName, e.message ?: "Unknown error")
                luaState.push(false)
                return@push 1
            }
        }
        lua.setField(-2, "create_table")

        // 插入数据
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(-1)
                return@push 1
            }

            val tableName = luaState.toString(1) ?: ""
            if (!luaState.isTable(2)) {
                luaState.push(-1)
                return@push 1
            }

            val values = mutableMapOf<String, Any>()
            val callbackIndex = if (luaState.top >= 3 && luaState.isFunction(3)) 3 else -1

            // 遍历Lua表获取值
            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val key = luaState.toString(-2)
                if (key != null) {
                    val value = when {
                        luaState.isString(-1) -> luaState.toString(-1) ?: ""
                        luaState.isInteger(-1) -> luaState.toInteger(-1)
                        luaState.isNumber(-1) -> luaState.toNumber(-1)
                        luaState.isBoolean(-1) -> luaState.toBoolean(-1)
                        else -> luaState.toString(-1) ?: ""
                    }
                    values[key] = value
                }
                luaState.pop(1)
            }

            if (callbackIndex != -1) {
                luaState.pushValue(callbackIndex)
                val callbackRef = luaState.ref()

                mysqlManager.insert(tableName, values) { id ->
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        luaState.push(id)
                        luaState.pCall(1, 0)
                    }
                    luaState.unref(callbackRef)
                }
            }

            luaState.pushNil()
            return@push 1
        }
        lua.setField(-2, "insert")

        // 更新数据
        lua.push { luaState ->
            if (luaState.top < 4) {
                luaState.push(-1)
                return@push 1
            }

            val tableName = luaState.toString(1) ?: ""
            if (!luaState.isTable(2)) {
                luaState.push(-1)
                return@push 1
            }
            val where = luaState.toString(3) ?: ""
            if (!luaState.isTable(4)) {
                luaState.push(-1)
                return@push 1
            }

            val callbackIndex = if (luaState.top >= 5 && luaState.isFunction(5)) 5 else -1

            val values = mutableMapOf<String, Any>()
            val whereArgs = mutableListOf<Any>()

            // 遍历Lua表获取更新值
            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val key = luaState.toString(-2)
                if (key != null) {
                    val value = when {
                        luaState.isString(-1) -> luaState.toString(-1) ?: ""
                        luaState.isInteger(-1) -> luaState.toInteger(-1)
                        luaState.isNumber(-1) -> luaState.toNumber(-1)
                        luaState.isBoolean(-1) -> luaState.toBoolean(-1)
                        else -> luaState.toString(-1) ?: ""
                    }
                    values[key] = value
                }
                luaState.pop(1)
            }

            // 遍历Lua表获取where参数
            luaState.pushNil()
            while (luaState.next(4) != 0) {
                val value = when {
                    luaState.isString(-1) -> luaState.toString(-1) ?: ""
                    luaState.isInteger(-1) -> luaState.toInteger(-1)
                    luaState.isNumber(-1) -> luaState.toNumber(-1)
                    luaState.isBoolean(-1) -> luaState.toBoolean(-1)
                    else -> luaState.toString(-1) ?: ""
                }
                whereArgs.add(value)
                luaState.pop(1)
            }

            if (callbackIndex != -1) {
                luaState.pushValue(callbackIndex)
                val callbackRef = luaState.ref()

                mysqlManager.update(tableName, values, where, whereArgs) { affectedRows ->
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        luaState.push(affectedRows)
                        luaState.pCall(1, 0)
                    }
                    luaState.unref(callbackRef)
                }
            }

            luaState.pushNil()
            return@push 1
        }
        lua.setField(-2, "update")

        // 查询数据
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val tableName = luaState.toString(1) ?: ""
            val columns = if (luaState.top >= 2 && luaState.isTable(2)) {
                val list = mutableListOf<String>()
                luaState.pushNil()
                while (luaState.next(2) != 0) {
                    val value = luaState.toString(-1)
                    if (value != null) {
                        list.add(value)
                    }
                    luaState.pop(1)
                }
                list
            } else {
                listOf("*")
            }

            val where = if (luaState.top >= 3 && luaState.isString(3)) {
                luaState.toString(3)
            } else {
                null
            }

            val whereArgs = if (luaState.top >= 4 && luaState.isTable(4)) {
                val list = mutableListOf<Any>()
                luaState.pushNil()
                while (luaState.next(4) != 0) {
                    val value = when {
                        luaState.isString(-1) -> luaState.toString(-1) ?: ""
                        luaState.isInteger(-1) -> luaState.toInteger(-1)
                        luaState.isNumber(-1) -> luaState.toNumber(-1)
                        luaState.isBoolean(-1) -> luaState.toBoolean(-1)
                        else -> luaState.toString(-1) ?: ""
                    }
                    list.add(value)
                    luaState.pop(1)
                }
                list
            } else {
                emptyList()
            }

            val callbackIndex = if (luaState.top >= 5 && luaState.isFunction(5)) 5 else -1

            if (callbackIndex != -1) {
                luaState.pushValue(callbackIndex)
                val callbackRef = luaState.ref()

                mysqlManager.query(tableName, columns, where, whereArgs) { results ->
                    luaState.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    if (luaState.isFunction(-1)) {
                        luaState.newTable()
                        results.forEachIndexed { index, row ->
                            luaState.newTable()
                            row.forEach { (key, value) ->
                                when (value) {
                                    is String -> luaState.push(value)
                                    is Int -> luaState.push(value.toLong())
                                    is Double -> luaState.push(value)
                                    is Boolean -> luaState.push(value)
                                    else -> luaState.push(value.toString())
                                }
                                luaState.setField(-2, key)
                            }
                            luaState.rawSetI(-2, index + 1)
                        }
                        luaState.pCall(1, 0)
                    }
                    luaState.unref(callbackRef)
                }
            }

            luaState.pushNil()
            return@push 1
        }
        lua.setField(-2, "query")

        lua.setGlobal("mysql")

        // 添加到 API 名称列表
        if (!apiNames.contains("mysql")) {
            apiNames.add("mysql")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 