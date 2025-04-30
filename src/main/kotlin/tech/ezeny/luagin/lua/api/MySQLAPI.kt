package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.config.MySQLManager
import tech.ezeny.luagin.utils.PLog

object MySQLAPI : LuaAPIProvider, KoinComponent {
    private val mysqlManager: MySQLManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(globals: Globals) {
        // 创建 mysql 表
        val mysqlTable = LuaTable()
        globals.set("mysql", mysqlTable)

        // 创建表
        mysqlTable.set("create_table", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).istable()) {
                    FALSE
                }

                val tableName = args.checkjstring(1)
                val columnsTable = args.checktable(2)
                val columns = mutableMapOf<String, String>()

                // 遍历Lua表获取列定义
                var i = 1
                while (true) {
                    val key = columnsTable.get(i)
                    val value = columnsTable.get(i + 1)
                    if (key.isnil() || value.isnil()) break

                    columns[key.checkjstring()] = value.checkjstring()
                    i += 2
                }

                return try {
                    mysqlManager.createTable(tableName, columns)
                    TRUE
                } catch (e: Exception) {
                    PLog.warning("log.warning.mysql_create_table_failed", tableName, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 插入数据
        mysqlTable.set("insert", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).istable()) {
                    return valueOf(-1)
                }

                val tableName = args.checkjstring(1)
                val valuesTable = args.checktable(2)
                val values = mutableMapOf<String, Any>()
                val callback = if (args.narg() >= 3 && args.arg(3).isfunction()) args.arg(3) else null

                // 遍历Lua表获取值
                var i = 1
                while (true) {
                    val key = valuesTable.get(i)
                    val value = valuesTable.get(i + 1)
                    if (key.isnil() || value.isnil()) break

                    values[key.checkjstring()] = when {
                        value.isstring() -> value.checkjstring()
                        value.isint() -> value.checkint()
                        value.isnumber() -> value.checkdouble()
                        value.isboolean() -> value.checkboolean()
                        else -> value.tojstring()
                    }
                    i += 2
                }

                mysqlManager.insert(tableName, values) { id ->
                    callback?.invoke(valueOf(id))
                }
                return NIL
            }
        })

        // 更新数据
        mysqlTable.set("update", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 4 || !args.arg(1).isstring() || !args.arg(2).istable() ||
                    !args.arg(3).isstring() || !args.arg(4).istable()
                ) {
                    return valueOf(-1)
                }

                val tableName = args.checkjstring(1)
                val valuesTable = args.checktable(2)
                val where = args.checkjstring(3)
                val whereArgsTable = args.checktable(4)
                val callback = if (args.narg() >= 5 && args.arg(5).isfunction()) args.arg(5) else null

                val values = mutableMapOf<String, Any>()
                val whereArgs = mutableListOf<Any>()

                // 遍历Lua表获取更新值
                var i = 1
                while (true) {
                    val key = valuesTable.get(i)
                    val value = valuesTable.get(i + 1)
                    if (key.isnil() || value.isnil()) break

                    values[key.checkjstring()] = when {
                        value.isstring() -> value.checkjstring()
                        value.isint() -> value.checkint()
                        value.isnumber() -> value.checkdouble()
                        value.isboolean() -> value.checkboolean()
                        else -> value.tojstring()
                    }
                    i += 2
                }

                // 遍历Lua表获取where参数
                i = 1
                while (true) {
                    val value = whereArgsTable.get(i)
                    if (value.isnil()) break

                    whereArgs.add(
                        when {
                            value.isstring() -> value.checkjstring()
                            value.isint() -> value.checkint()
                            value.isnumber() -> value.checkdouble()
                            value.isboolean() -> value.checkboolean()
                            else -> value.tojstring()
                        }
                    )
                    i++
                }

                mysqlManager.update(tableName, values, where, whereArgs) { affectedRows ->
                    callback?.invoke(valueOf(affectedRows))
                }
                return NIL
            }
        })

        // 查询数据
        mysqlTable.set("query", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val tableName = args.checkjstring(1)
                val columns = if (args.narg() >= 2 && args.arg(2).istable()) {
                    val columnsTable = args.checktable(2)
                    val list = mutableListOf<String>()
                    var i = 1
                    while (true) {
                        val value = columnsTable.get(i)
                        if (value.isnil()) break
                        list.add(value.checkjstring())
                        i++
                    }
                    list
                } else {
                    listOf("*")
                }

                val where = if (args.narg() >= 3 && args.arg(3).isstring()) {
                    args.checkjstring(3)
                } else {
                    null
                }

                val whereArgs = if (args.narg() >= 4 && args.arg(4).istable()) {
                    val whereArgsTable = args.checktable(4)
                    val list = mutableListOf<Any>()
                    var i = 1
                    while (true) {
                        val value = whereArgsTable.get(i)
                        if (value.isnil()) break
                        list.add(
                            when {
                                value.isstring() -> value.checkjstring()
                                value.isint() -> value.checkint()
                                value.isnumber() -> value.checkdouble()
                                value.isboolean() -> value.checkboolean()
                                else -> value.tojstring()
                            }
                        )
                        i++
                    }
                    list
                } else {
                    emptyList()
                }

                val callback = if (args.narg() >= 5 && args.arg(5).isfunction()) args.arg(5) else null

                mysqlManager.query(tableName, columns, where, whereArgs) { results ->
                    val resultTable = LuaTable()
                    results.forEachIndexed { index, row ->
                        val rowTable = LuaTable()
                        row.forEach { (key, value) ->
                            rowTable.set(
                                key, when (value) {
                                    is String -> valueOf(value)
                                    is Int -> valueOf(value)
                                    is Double -> valueOf(value)
                                    is Boolean -> valueOf(value)
                                    else -> valueOf(value.toString())
                                }
                            )
                        }
                        resultTable.set(index + 1, rowTable)
                    }
                    callback?.invoke(resultTable)
                }
                return NIL
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("mysql")) {
            apiNames.add("mysql")
        }

        PLog.info("log.info.mysql_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
} 