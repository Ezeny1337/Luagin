package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.PLog

object YamlAPI : LuaAPIProvider, KoinComponent {
    private val yamlManager: YamlManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 yaml 表
        lua.newTable()

        // 获取配置值
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.pushNil()
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val key = luaState.toString(2) ?: ""

            val config = yamlManager.getConfig(relativePath)
            if (config == null) {
                luaState.pushNil()
                return@push 1
            }

            if (!config.contains(key)) {
                luaState.pushNil()
                return@push 1
            }

            // 转换不同类型的值为 Lua 值
            when {
                config.isString(key) -> luaState.push(config.getString(key) ?: "")
                config.isInt(key) -> luaState.push(config.getInt(key).toLong())
                config.isDouble(key) -> luaState.push(config.getDouble(key))
                config.isBoolean(key) -> luaState.push(config.getBoolean(key))
                config.isList(key) -> {
                    val list = config.getList(key)
                    if (list != null) {
                        luaState.newTable()
                        list.forEachIndexed { index, value ->
                            LuaValueFactory.pushJavaObject(luaState, value)
                            luaState.rawSetI(-2, index + 1)
                        }
                    } else {
                        luaState.pushNil()
                    }
                }
                else -> luaState.pushNil()
            }
            return@push 1
        }
        lua.setField(-2, "get")
        
        // 设置配置值
        lua.push { luaState ->
            if (luaState.top < 3) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val key = luaState.toString(2) ?: ""

            val config = yamlManager.getConfig(relativePath)
            if (config == null) {
                luaState.push(false)
                return@push 1
            }

            // 递归处理 Lua 表
            fun convertToYaml(index: Int): Any {
                return when {
                    luaState.isNil(index) -> ""
                    luaState.isString(index) -> luaState.toString(index) ?: ""
                    luaState.isInteger(index) -> luaState.toInteger(index)
                    luaState.isNumber(index) -> luaState.toNumber(index)
                    luaState.isBoolean(index) -> luaState.toBoolean(index)
                    luaState.isTable(index) -> {
                        val map = mutableMapOf<String, Any>()
                        luaState.pushNil()
                        while (luaState.next(index) != 0) {
                            val keyStr = luaState.toString(-2) ?: ""
                            val value = convertToYaml(-1)
                            map[keyStr] = value
                            luaState.pop(1)
                        }
                        map
                    }
                    else -> luaState.toString(index) ?: ""
                }
            }

            // 转换 Lua 值为 YAML 支持的类型
            config.set(key, convertToYaml(3))

            // 自动保存配置文件
            val success = yamlManager.saveConfig(relativePath)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "set")

        lua.setGlobal("yaml")
        
        // 添加到 API 名称列表
        if (!apiNames.contains("yaml")) {
            apiNames.add("yaml")
        }
    }
    
    override fun getAPINames(): List<String> = apiNames
}