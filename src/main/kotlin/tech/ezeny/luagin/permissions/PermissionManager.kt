package tech.ezeny.luagin.permissions

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import tech.ezeny.luagin.config.YamlManager
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.permissions.Permission
import org.bukkit.permissions.PermissionAttachment
import org.bukkit.plugin.PluginManager
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class PermissionManager(private val plugin: Luagin, private val yamlManager: YamlManager) {
    private val configFile = "configs/permissions.yml"
    private val playerPermissions = ConcurrentHashMap<UUID, PermissionAttachment>()
    private val pluginManager: PluginManager = Bukkit.getPluginManager()
    private val registeredPermissions = mutableSetOf<String>()

    init {
        ensureConfigFileExists()
        registerBukkitPermissions()

        // 注册玩家加入事件监听器
        Bukkit.getPluginManager().registerEvents(object : org.bukkit.event.Listener {
            @org.bukkit.event.EventHandler
            fun onPlayerJoin(event: org.bukkit.event.player.PlayerJoinEvent) {
                setupPlayerPermissions(event.player)
            }

            @org.bukkit.event.EventHandler
            fun onPlayerQuit(event: org.bukkit.event.player.PlayerQuitEvent) {
                removePlayerPermissions(event.player)
            }
        }, plugin)
    }

    private fun ensureConfigFileExists() {
        val config = yamlManager.getConfig(configFile)
        if (config == null) {
            // 创建默认配置文件
            val defaultConfig = """
                groups:
                  default:
                    weight: 0
                    permissions:
                      - luagin.user
                  admin:
                    weight: 100
                    inherit:
                      - default
                    permissions:
                      - luagin.admin
                players: {}
            """.trimIndent()

            // 创建配置文件
            val file = File(plugin.dataFolder, configFile)
            file.parentFile.mkdirs()
            file.writeText(defaultConfig)
        }
    }

    /**
     * 注册所有权限到 Bukkit 权限系统
     */
    private fun registerBukkitPermissions() {
        val config = yamlManager.getConfig(configFile) ?: return

        // 注册所有组权限
        config.getConfigurationSection("groups")?.getKeys(false)?.forEach { groupName ->
            val permissions = config.getStringList("groups.$groupName.permissions")
            permissions.forEach { perm ->
                registerPermission(perm)
            }
        }
    }

    /**
     * 向 Bukkit 注册单个权限
     */
    private fun registerPermission(permission: String) {
        if (permission in registeredPermissions) return

        try {
            if (pluginManager.getPermission(permission) == null) {
                val perm = Permission(permission)
                pluginManager.addPermission(perm)
                registeredPermissions.add(permission)
            }
        } catch (e: Exception) {
            PLog.warning("注册权限失败: $permission", e.message ?: "未知错误")
        }
    }

    /**
     * 获取权限配置文件
     */
    fun getConfig(): YamlConfiguration? {
        return yamlManager.getConfig(configFile)
    }

    /**
     * 检查命令发送者是否拥有指定权限
     */
    fun hasPermission(sender: CommandSender, permission: String): Boolean {
        if (sender is ConsoleCommandSender) {
            return true
        }

        return sender.hasPermission(permission)
    }

    /**
     * 为玩家设置权限
     */
    fun setupPlayerPermissions(player: Player) {
        // 移除旧的权限附件
        removePlayerPermissions(player)

        // 创建新的权限附件
        val attachment = player.addAttachment(plugin)
        playerPermissions[player.uniqueId] = attachment

        val config = yamlManager.getConfig(configFile) ?: return
        val uuid = player.uniqueId.toString()

        // 设置玩家特定权限
        if (config.contains("players.$uuid.permissions")) {
            val playerPermissions = config.getStringList("players.$uuid.permissions")
            playerPermissions.forEach { perm ->
                registerPermission(perm)
                attachment.setPermission(perm, true)
            }
        }

        // 获取玩家所属的所有权限组（包括继承的）
        val playerGroups = getPlayerGroups(uuid, config)

        // 按权重排序权限组
        val sortedGroups = playerGroups.sortedByDescending { group ->
            config.getInt("groups.$group.weight", 0)
        }

        // 设置每个权限组的权限
        for (group in sortedGroups) {
            if (config.contains("groups.$group.permissions")) {
                val groupPermissions = config.getStringList("groups.$group.permissions")
                groupPermissions.forEach { perm ->
                    registerPermission(perm)
                    attachment.setPermission(perm, true)
                }
            }
        }

        // 设置默认权限组的权限
        val defaultPermissions = config.getStringList("groups.default.permissions")
        defaultPermissions.forEach { perm ->
            registerPermission(perm)
            attachment.setPermission(perm, true)
        }

        // 重新计算权限
        player.recalculatePermissions()
    }

    /**
     * 移除玩家的权限附件
     */
    private fun removePlayerPermissions(player: Player) {
        playerPermissions[player.uniqueId]?.let { attachment ->
            try {
                player.removeAttachment(attachment)
            } catch (e: Exception) {
                // 忽略可能的错误
            }
            playerPermissions.remove(player.uniqueId)
        }
    }

    /**
     * 获取玩家所属的所有权限组（包括继承的）
     */
    private fun getPlayerGroups(uuid: String, config: YamlConfiguration): Set<String> {
        val groups = mutableSetOf<String>()

        // 获取直接所属的权限组
        if (config.contains("players.$uuid.groups")) {
            groups.addAll(config.getStringList("players.$uuid.groups"))
        }

        // 递归获取继承的权限组
        val inheritedGroups = mutableSetOf<String>()
        groups.forEach { group ->
            getInheritedGroups(group, config, inheritedGroups)
        }
        groups.addAll(inheritedGroups)

        return groups
    }

    /**
     * 递归获取权限组继承的所有权限组
     */
    private fun getInheritedGroups(
        group: String,
        config: YamlConfiguration,
        result: MutableSet<String>
    ) {
        if (config.contains("groups.$group.inherit")) {
            val inherited = config.getStringList("groups.$group.inherit")
            inherited.forEach { inheritedGroup ->
                if (result.add(inheritedGroup)) {
                    getInheritedGroups(inheritedGroup, config, result)
                }
            }
        }
    }

    /**
     * 添加玩家权限
     */
    fun addPermission(player: Player, permission: String): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        val uuid = player.uniqueId.toString()

        val path = "players.$uuid.permissions"
        val permissions = config.getStringList(path).toMutableList()

        if (!permissions.contains(permission)) {
            permissions.add(permission)
            config.set(path, permissions)

            // 注册权限
            registerPermission(permission)

            // 更新玩家权限
            playerPermissions[player.uniqueId]?.setPermission(permission, true)
            player.recalculatePermissions()

            return yamlManager.saveConfig(configFile)
        }
        return true
    }

    /**
     * 移除玩家权限
     */
    fun removePermission(player: Player, permission: String): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        val uuid = player.uniqueId.toString()

        val path = "players.$uuid.permissions"
        val permissions = config.getStringList(path).toMutableList()

        if (permissions.remove(permission)) {
            config.set(path, permissions)

            // 更新玩家权限
            playerPermissions[player.uniqueId]?.unsetPermission(permission)
            player.recalculatePermissions()

            return yamlManager.saveConfig(configFile)
        }
        return true
    }

    /**
     * 添加权限组
     */
    fun addGroup(
        groupName: String,
        permissions: List<String>,
        weight: Int = 0,
        inherit: List<String> = emptyList()
    ): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        config.set("groups.$groupName.permissions", permissions)
        config.set("groups.$groupName.weight", weight)
        config.set("groups.$groupName.inherit", inherit)

        // 注册所有权限
        permissions.forEach { registerPermission(it) }

        // 更新所有玩家的权限
        Bukkit.getOnlinePlayers().forEach { player ->
            setupPlayerPermissions(player)
        }

        return yamlManager.saveConfig(configFile)
    }

    /**
     * 将玩家添加到权限组
     */
    fun addPlayerToGroup(player: Player, groupName: String): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        val uuid = player.uniqueId.toString()

        val path = "players.$uuid.groups"
        val groups = config.getStringList(path).toMutableList()

        if (!groups.contains(groupName)) {
            groups.add(groupName)
            config.set(path, groups)

            // 更新玩家权限
            setupPlayerPermissions(player)

            return yamlManager.saveConfig(configFile)
        }
        return true
    }

    /**
     * 将玩家从权限组移除
     */
    fun removePlayerFromGroup(player: Player, groupName: String): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        val uuid = player.uniqueId.toString()

        val path = "players.$uuid.groups"
        val groups = config.getStringList(path).toMutableList()

        if (groups.remove(groupName)) {
            config.set(path, groups)

            // 更新玩家权限
            setupPlayerPermissions(player)

            return yamlManager.saveConfig(configFile)
        }
        return true
    }

    /**
     * 获取权限组权重
     */
    fun getGroupWeight(groupName: String): Int {
        val config = yamlManager.getConfig(configFile) ?: return 0
        return config.getInt("groups.$groupName.weight", 0)
    }

    /**
     * 设置权限组权重
     */
    fun setGroupWeight(groupName: String, weight: Int): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        config.set("groups.$groupName.weight", weight)
        return yamlManager.saveConfig(configFile)
    }

    /**
     * 获取权限组继承关系
     */
    fun getGroupInheritance(groupName: String): List<String> {
        val config = yamlManager.getConfig(configFile) ?: return emptyList()
        return config.getStringList("groups.$groupName.inherit")
    }

    /**
     * 设置权限组继承关系
     */
    fun setGroupInheritance(groupName: String, inherit: List<String>): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        config.set("groups.$groupName.inherit", inherit)

        // 更新所有玩家的权限
        Bukkit.getOnlinePlayers().forEach { player ->
            setupPlayerPermissions(player)
        }

        return yamlManager.saveConfig(configFile)
    }

    /**
     * 插件禁用时清理资源
     */
    fun cleanup() {
        // 移除所有玩家的权限附件
        playerPermissions.forEach { (_, attachment) ->
            attachment.remove()
            playerPermissions.clear()
        }

        // 移除所有注册的权限
        registeredPermissions.forEach { permission ->
            val perm = pluginManager.getPermission(permission)
            if (perm != null) {
                pluginManager.removePermission(perm)
            }
            registeredPermissions.clear()
        }
    }
}