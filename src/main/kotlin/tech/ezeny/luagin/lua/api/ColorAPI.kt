package tech.ezeny.luagin.lua.api

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.ColorUtils

object ColorAPI : LuaAPIProvider {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // gradient(text: string, start_color: string, end_color: string[, middle_color: string]) - 创建渐变文本
        lua.push { luaState ->
            if (luaState.top < 3) {
                luaState.push("")
                return@push 1
            }

            val text = luaState.toString(1) ?: ""
            val startColor = luaState.toString(2) ?: ""
            val endColor = luaState.toString(3) ?: ""
            val middleColor = if (luaState.top >= 4 && !luaState.isNil(4)) {
                luaState.toString(4)
            } else {
                null
            }

            val gradientText = if (middleColor != null) {
                // 三色渐变
                ColorUtils.createGradient(text, startColor, middleColor, endColor)
            } else {
                // 双色渐变
                ColorUtils.createGradient(text, startColor, endColor)
            }
            luaState.push(gradientText)

            return@push 1
        }
        lua.setGlobal("gradient")

        // 添加API名称到列表
        val allApiNames = listOf("gradient")
        allApiNames.forEach { name ->
            if (!apiNames.contains(name)) {
                apiNames.add(name)
            }
        }
    }

    override fun getAPINames(): List<String> = apiNames
}