package tech.ezeny.luagin.lua

import org.luaj.vm2.Globals
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.api.ActionBarAPI
import tech.ezeny.luagin.lua.api.ChatAPI
import tech.ezeny.luagin.lua.api.CommunicationAPI
import tech.ezeny.luagin.lua.api.EventsAPI
import tech.ezeny.luagin.lua.api.FilesAPI
import tech.ezeny.luagin.lua.api.MySQLAPI
import tech.ezeny.luagin.lua.api.CommonAPI
import tech.ezeny.luagin.lua.api.TitleAPI
import tech.ezeny.luagin.lua.api.UtilsAPI
import tech.ezeny.luagin.lua.api.YamlAPI

class APIRegister(private val plugin: Luagin) {
    // API 名称列表，用于共享
    val apiNames = mutableListOf<String>()

    // API提供者列表
    private val apiProviders = listOf(
        EventsAPI,
        ChatAPI,
        TitleAPI,
        ActionBarAPI,
        UtilsAPI,
        FilesAPI,
        YamlAPI,
        CommunicationAPI,
        MySQLAPI,
        CommonAPI
    )

    init {
        // 初始化所有API提供者
        apiProviders.forEach { it.initialize(plugin) }
    }

    /**
     * 注册所有 API 到 Lua 环境
     */
    fun registerAllAPIs(globals: Globals) {
        // 注册所有API提供者的API
        apiProviders.forEach { provider ->
            provider.registerAPI(globals)

            // 收集API名称
            val names = provider.getAPINames()
            names.forEach { name ->
                if (!apiNames.contains(name)) {
                    apiNames.add(name)
                }
            }
        }
    }
}