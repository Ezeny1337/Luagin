package tech.ezeny.luaKit.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals

object APIRegister {
    private lateinit var plugin: JavaPlugin

    // API提供者列表
    private val apiProviders = listOf(
        EventsAPI,
        ChatAPI,
        TitleAPI,
        ActionBarAPI,
        UtilsAPI,
        FilesAPI,
        YamlAPI
    )

    // API 名称列表，用于共享
    val apiNames = mutableListOf<String>()

    /**
     * 初始化 API 系统
     */
    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin

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