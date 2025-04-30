package tech.ezeny.luagin.permissions

import org.bukkit.entity.Player
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.ezeny.luagin.config.YamlManager
import java.io.File
import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.Luagin

class PermissionManager(plugin: Luagin) : KoinComponent {
    private val yamlManager: YamlManager by inject()
    private val configFile = "configs/permissions.yml"

    init {
        // 确保权限配置文件存在并初始化
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
                      - luagin.mod
                players: {}
            """.trimIndent()

            // 创建配置文件
            val configFile = File(plugin.dataFolder, configFile)
            configFile.parentFile.mkdirs()
            configFile.writeText(defaultConfig)
        }
    }

    /**
     * 获取权限配置文件
     */
    fun getConfig(): YamlConfiguration? {
        return yamlManager.getConfig(configFile)
    }

    /**
     * 检查玩家是否拥有指定权限
     */
    fun hasPermission(player: Player, permission: String): Boolean {
        val config = yamlManager.getConfig(configFile) ?: return false
        val uuid = player.uniqueId.toString()

        // 检查玩家特定权限
        if (config.contains("players.$uuid.permissions")) {
            val playerPermissions = config.getStringList("players.$uuid.permissions")
            if (playerPermissions.contains(permission)) return true
        }

        // 获取玩家所属的所有权限组（包括继承的）
        val playerGroups = getPlayerGroups(uuid, config)

        // 按权重排序权限组
        val sortedGroups = playerGroups.sortedByDescending { group ->
            config.getInt("groups.$group.weight", 0)
        }

        // 检查每个权限组
        for (group in sortedGroups) {
            if (config.contains("groups.$group.permissions")) {
                val groupPermissions = config.getStringList("groups.$group.permissions")
                if (groupPermissions.contains(permission)) return true
            }
        }

        // 检查默认权限组
        val defaultPermissions = config.getStringList("groups.default.permissions")
        return defaultPermissions.contains(permission)
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
        return yamlManager.saveConfig(configFile)
    }
} 