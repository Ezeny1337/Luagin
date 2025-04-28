package tech.ezeny.luaKit.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luaKit.config.YamlManager
import tech.ezeny.luaKit.utils.PLog
import java.io.File
import java.io.IOException

object YamlAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()
    
    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }
    
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
                val absolutePath = getAbsolutePath(relativePath)

                // 自动加载配置文件
                val config = YamlManager.getConfigFromPath(absolutePath) ?: return NIL

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
                            luaTable.set(index + 1, valueOf(value.toString()))
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
                val absolutePath = getAbsolutePath(relativePath)

                // 自动加载或创建配置文件
                val config = YamlManager.getConfigFromPath(absolutePath) ?: return valueOf(false)

                // 根据 Lua 值类型设置不同类型的配置值
                when {
                    value.isnil() -> config.set(key, null)
                    value.isstring() -> config.set(key, value.tojstring())
                    value.isint() -> config.set(key, value.toint())
                    value.isnumber() -> config.set(key, value.todouble())
                    value.isboolean() -> config.set(key, value.toboolean())
                    value.istable() -> {
                        // 将 Lua 表转换为列表
                        val list = mutableListOf<String>()
                        var i = 1
                        while (true) {
                            val item = value.get(i)
                            if (item.isnil()) break
                            list.add(item.tojstring())
                            i++
                        }
                        config.set(key, list)
                    }
                }

                // 自动保存配置文件
                val success = YamlManager.saveConfigToPath(absolutePath)
                return valueOf(success)
            }
        })
        
        // 添加到 API 名称列表
        if (!apiNames.contains("yaml")) {
            apiNames.add("yaml")
        }
        
        PLog.info("log.info.yaml_api_set")
    }
    
    /**
     * 获取绝对路径
     * 将相对路径转换为基于插件目录的绝对路径
     */
    private fun getAbsolutePath(relativePath: String): String {
        // 如果已经是绝对路径并且在插件目录内，直接返回
        val file = File(relativePath)
        if (file.isAbsolute && isPathSafe(file)) {
            return relativePath
        }
        
        // 否则，将其视为相对于插件目录的路径
        return File(plugin.dataFolder, relativePath).absolutePath
    }

    /**
     * 安全检查：确保路径在插件目录内
     * 仅用于验证绝对路径
     */
    private fun isPathSafe(file: File): Boolean {
        try {
            val pluginDirPath = plugin.dataFolder.canonicalPath
            val filePath = file.canonicalPath
            return filePath.startsWith(pluginDirPath)
        } catch (e: IOException) {
            return false
        }
    }
    
    override fun getAPINames(): List<String> = apiNames
}