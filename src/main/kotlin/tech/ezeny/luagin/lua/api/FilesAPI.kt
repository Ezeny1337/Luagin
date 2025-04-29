package tech.ezeny.luagin.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.utils.PLog
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

        // 读取文件内容
        filesTable.set("read", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val file = File(absolutePath)

                if (!file.exists() || !file.isFile) {
                    return NIL
                }

                return try {
                    val content = file.readText(Charsets.UTF_8)
                    valueOf(content)
                } catch (e: IOException) {
                    PLog.warning("log.warning.read_file_error", relativePath, e.message ?: "Unknown error")
                    NIL
                }
            }
        })

        // 写入文件内容
        filesTable.set("write", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val contents = args.checkjstring(2)
                val isBinary = if (args.narg() > 2) args.checkboolean(3) else false

                val absolutePath = getAbsolutePath(relativePath)
                val file = File(absolutePath)

                return try {
                    // 二进制或文本写入
                    if (isBinary) {
                        file.writeBytes(contents.toByteArray())
                    } else {
                        file.writeText(contents, Charsets.UTF_8)
                    }
                    TRUE
                } catch (e: IOException) {
                    PLog.warning("log.warning.write_file_error", relativePath, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 创建目录
        filesTable.set("create_folder", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val dir = File(absolutePath)

                return try {
                    valueOf(dir.mkdirs())
                } catch (e: IOException) {
                    PLog.warning("log.warning.mkdirs_error", relativePath, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 创建文件
        filesTable.set("create_file", object : VarArgFunction() {
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
                    PLog.warning("log.warning.create_file_error", relativePath, e.message ?: "Unknown error")
                    FALSE
                }
            }
        })

        // 检查文件或文件夹是否存在
        filesTable.set("exists", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val relativePath = args.checkjstring(1)
                val absolutePath = getAbsolutePath(relativePath)
                val file = File(absolutePath)

                return valueOf(file.exists())
            }
        })

        // 列出目录内容
        filesTable.set("list_dir", object : VarArgFunction() {
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