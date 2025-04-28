package tech.ezeny.luaKit.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luaKit.utils.PLog
import java.io.File
import java.io.IOException

object FilesAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        // 创建 files 表
        val filesTable = LuaTable()
        globals.set("files", filesTable)

        // 检查目录是否存在
        filesTable.set("dirExists", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val dir = File(absolutePath)

                return valueOf(dir.exists() && dir.isDirectory)
            }
        })

        // 创建目录
        filesTable.set("createDir", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val dir = File(absolutePath)

                if (dir.exists()) {
                    return TRUE
                }

                return try {
                    valueOf(dir.mkdirs())
                } catch (e: IOException) {
                    PLog.severe("log.warning.mkdirs_error", relativePath, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 创建文件
        filesTable.set("createFile", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val file = File(absolutePath)

                if (file.exists()) {
                    return TRUE
                }

                return try {
                    valueOf(file.createNewFile())
                } catch (e: IOException) {
                    PLog.severe("log.warning.create_file_error", relativePath, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 检查文件是否存在
        filesTable.set("fileExists", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val file = File(absolutePath)

                return valueOf(file.exists() && file.isFile)
            }
        })

        // 列出目录内容
        filesTable.set("listDir", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val dir = File(absolutePath)

                if (!dir.exists() || !dir.isDirectory) {
                    PLog.warning("log.warning.dir_not_found", relativePath)
                    return NIL
                }

                val files = dir.listFiles() ?: return NIL
                val luaTable = LuaTable()

                files.forEachIndexed { index, file ->
                    luaTable.set(index + 1, valueOf(file.name))
                }

                return luaTable
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("files")) {
            apiNames.add("files")
        }

        PLog.info("log.info.files_api_set")
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
     * 安全检查
     * 确保路径在插件目录内
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