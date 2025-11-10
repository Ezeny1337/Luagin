package tech.ezeny.luagin.lua

import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.api.ColorAPI
import tech.ezeny.luagin.lua.api.CommandAPI
import tech.ezeny.luagin.lua.api.CommunicationAPI
import tech.ezeny.luagin.lua.api.EventsAPI
import tech.ezeny.luagin.lua.api.FilesAPI
import tech.ezeny.luagin.lua.api.GlobalsAPI
import tech.ezeny.luagin.lua.api.GuiAPI
import tech.ezeny.luagin.lua.api.ItemsAPI
import tech.ezeny.luagin.lua.api.MessageAPI
import tech.ezeny.luagin.lua.api.MySQLAPI
import tech.ezeny.luagin.lua.api.NetworkAPI
import tech.ezeny.luagin.lua.api.PermissionAPI
import tech.ezeny.luagin.lua.api.ProtocolAPI
import tech.ezeny.luagin.lua.api.UtilsAPI
import tech.ezeny.luagin.lua.api.YamlAPI

class APIRegister(private val plugin: Luagin) {
    // API 名称列表，用于共享
    val apiNames = mutableListOf<String>()

    // apiProviders 列表
    private val apiProviders = listOf(
        ColorAPI,
        EventsAPI,
        MessageAPI,
        UtilsAPI,
        FilesAPI,
        YamlAPI,
        CommunicationAPI,
        MySQLAPI,
        PermissionAPI,
        CommandAPI,
        NetworkAPI,
        ProtocolAPI,
        GlobalsAPI,
        ItemsAPI,
        GuiAPI
    )

    init {
        // 初始化所有 apiProviders
        apiProviders.forEach { it.initialize(plugin) }
    }

    /**
     * 注册所有 API 到 Lua 环境
     */
    fun registerAllAPIs(lua: Lua) {
        // 注册所有 API
        apiProviders.forEach { provider ->
            provider.registerAPI(lua)

            val names = provider.getAPINames()
            names.forEach { name ->
                if (!apiNames.contains(name)) {
                    apiNames.add(name)
                }
            }
        }
    }
}