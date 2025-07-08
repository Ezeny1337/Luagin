package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.io.File
import java.io.IOException

object FilesAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 files 表
        lua.newTable()

        // 读取文件内容
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val absolutePath = getAbsolutePath(relativePath)
            val file = File(absolutePath)

            if (!file.exists() || !file.isFile) {
                luaState.pushNil()
                return@push 1
            }

            try {
                val content = file.readText(Charsets.UTF_8)
                luaState.push(content)
                return@push 1
            } catch (e: IOException) {
                PLog.warning("log.warning.read_file_failed", relativePath, e.message ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "read")

        // 写入文件内容
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val contents = luaState.toString(2) ?: ""
            val isBinary = if (luaState.top > 2) luaState.toBoolean(3) else false

            val absolutePath = getAbsolutePath(relativePath)
            val file = File(absolutePath)

            try {
                // 二进制或文本写入
                if (isBinary) {
                    file.writeBytes(contents.toByteArray())
                } else {
                    file.writeText(contents, Charsets.UTF_8)
                }
                luaState.push(true)
                return@push 1
            } catch (e: IOException) {
                PLog.warning("log.warning.write_file_failed", relativePath, e.message ?: "Unknown error")
                luaState.push(false)
                return@push 1
            }
        }
        lua.setField(-2, "write")

        // 创建目录
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val absolutePath = getAbsolutePath(relativePath)
            val dir = File(absolutePath)

            try {
                luaState.push(dir.mkdirs())
                return@push 1
            } catch (e: IOException) {
                PLog.warning("log.warning.mkdirs_failed", relativePath, e.message ?: "Unknown error")
                luaState.push(false)
                return@push 1
            }
        }
        lua.setField(-2, "create_folder")

        // 创建文件
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val absolutePath = getAbsolutePath(relativePath)
            val file = File(absolutePath)

            if (file.exists()) {
                luaState.push(true)
                return@push 1
            }

            try {
                luaState.push(file.createNewFile())
                return@push 1
            } catch (e: IOException) {
                PLog.warning("log.warning.create_file_failed", relativePath, e.message ?: "Unknown error")
                luaState.push(false)
                return@push 1
            }
        }
        lua.setField(-2, "create_file")

        // 检查文件或文件夹是否存在
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val absolutePath = getAbsolutePath(relativePath)
            val file = File(absolutePath)

            luaState.push(file.exists())
            return@push 1
        }
        lua.setField(-2, "exists")

        // 列出目录内容
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val absolutePath = getAbsolutePath(relativePath)
            val dir = File(absolutePath)

            if (!dir.exists() || !dir.isDirectory) {
                PLog.warning("log.warning.dir_not_found", relativePath)
                luaState.pushNil()
                return@push 1
            }

            val files = dir.listFiles()
            if (files == null) {
                luaState.pushNil()
                return@push 1
            }

            luaState.newTable()
            files.forEachIndexed { index, file ->
                luaState.push(file.name)
                luaState.rawSetI(-2, index + 1)
            }

            return@push 1
        }
        lua.setField(-2, "list_dir")

        lua.setGlobal("files")

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
        val file = File(relativePath)
        if (file.isAbsolute && isPathSafe(file)) {
            return relativePath
        }

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