package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.FileUtils

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
            val content = FileUtils.readFile(relativePath)
            
            if (content != null) {
                luaState.push(content)
            } else {
                luaState.pushNil()
            }
            return@push 1
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

            val success = FileUtils.writeFile(relativePath, contents, isBinary)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "write")

        // 创建目录
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val success = FileUtils.createDirectory(relativePath)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "create_folder")

        // 创建文件
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val success = FileUtils.createFile(relativePath)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "create_file")

        // 检查文件或文件夹是否存在
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val exists = FileUtils.exists(relativePath)
            luaState.push(exists)
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
            val files = FileUtils.listDirectory(relativePath)
            
            if (files != null) {
                luaState.newTable()
                files.forEachIndexed { index, fileName ->
                    luaState.push(fileName)
                    luaState.rawSetI(-2, index + 1)
                }
            } else {
                luaState.pushNil()
            }

            return@push 1
        }
        lua.setField(-2, "list_dir")

        // 删除文件或目录
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val relativePath = luaState.toString(1) ?: ""
            val success = FileUtils.delete(relativePath)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "delete")

        lua.setGlobal("files")

        // 添加到 API 名称列表
        if (!apiNames.contains("files")) {
            apiNames.add("files")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}