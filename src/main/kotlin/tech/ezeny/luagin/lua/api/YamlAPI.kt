package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.lua.APIRegister.apiNames
import tech.ezeny.luagin.utils.PLog

object YamlAPI : LuaAPIProvider, KoinComponent {
    private val yamlManager: YamlManager by inject()

    override fun registerAPI(globals: Globals) {
        // 创建 yaml 表
        val yamlTable = LuaTable()
        globals.set("yaml", yamlTable)

        // 获取配置值
        yamlTable.set("get", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return NIL
                }

                val relativePath = args.checkjstring(1)
                val key = args.checkjstring(2)

                val config = yamlManager.getConfig(relativePath) ?: return NIL

                if (!config.contains(key)) {
                    return NIL
                }

                // 转换不同类型的值为 Lua 值
                return when {
                    config.isString(key) -> valueOf(config.getString(key))
                    config.isInt(key) -> valueOf(config.getInt(key))
                    config.isDouble(key) -> valueOf(config.getDouble(key))
                    config.isBoolean(key) -> valueOf(config.getBoolean(key))
                    config.isList(key) -> {
                        val list = config.getList(key) ?: return NIL
                        val luaTable = LuaTable()
                        list.forEachIndexed { index, value ->
                            luaTable.set(index + 1, when (value) {
                                is String -> valueOf(value)
                                is Int -> valueOf(value)
                                is Boolean -> valueOf(value)
                                is Double -> valueOf(value)
                                else -> valueOf(value.toString())
                            })
                        }
                        luaTable
                    }
                    else -> NIL
                }
            }
        })
        
        // 设置配置值
        yamlTable.set("set", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 3 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val relativePath = args.checkjstring(1)
                val key = args.checkjstring(2)
                val value = args.arg(3)

                val config = yamlManager.getConfig(relativePath) ?: return valueOf(false)

                // 递归处理 Lua 表
                fun convertToYaml(value: LuaValue): Any {
                    return when {
                        value.isnil() -> ""
                        value.isstring() -> value.tojstring()
                        value.isint() -> value.toint()
                        value.isnumber() -> value.todouble()
                        value.isboolean() -> value.toboolean()
                        value.istable() -> {
                            val map = mutableMapOf<String, Any>()
                            var i = 1
                            while (true) {
                                val item = value.get(i)
                                if (item.isnil()) break
                                map["$i"] = convertToYaml(item)
                                i++
                            }
                            map
                        }
                        else -> value.tojstring()
                    }
                }

                // 转换 Lua 值为 YAML 支持的类型
                config.set(key, convertToYaml(value))

                // 自动保存配置文件
                val success = yamlManager.saveConfig(relativePath)
                return valueOf(success)
            }
        })
        
        // 添加到 API 名称列表
        if (!apiNames.contains("yaml")) {
            apiNames.add("yaml")
        }
        
        PLog.info("log.info.yaml_api_set")
    }
    
    override fun getAPINames(): List<String> = apiNames
}