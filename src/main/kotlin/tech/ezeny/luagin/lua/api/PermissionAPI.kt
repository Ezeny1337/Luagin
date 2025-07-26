package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.permissions.PermissionManager

object PermissionAPI : LuaAPIProvider, KoinComponent {
    private val permissionManager: PermissionManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 perm 表
        lua.newTable()

        // player_check(player_name: string, permission: string): boolean - 检查玩家权限
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val permission = luaState.toString(2) ?: ""

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            luaState.push(player.hasPermission(permission))
            return@push 1
        }
        lua.setField(-2, "player_check")

        // player_add(player_name: string, permission: string): boolean - 给玩家添加权限
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val permission = luaState.toString(2) ?: ""

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            luaState.push(permissionManager.addPermission(player, permission))
            return@push 1
        }
        lua.setField(-2, "player_add")

        // player_remove(player_name: string, permission: string): boolean - 移除玩家权限
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val permission = luaState.toString(2) ?: ""

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            luaState.push(permissionManager.removePermission(player, permission))
            return@push 1
        }
        lua.setField(-2, "player_remove")

        // get_player_permissions(player_name: string): table - 获取玩家的所有权限
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.pushNil()
                return@push 1
            }

            val config = permissionManager.getConfig()
            if (config == null) {
                luaState.pushNil()
                return@push 1
            }

            val uuid = player.uniqueId.toString()
            val path = "players.$uuid.permissions"

            if (!config.contains(path)) {
                luaState.pushNil()
                return@push 1
            }

            val permissions = config.getStringList(path)
            luaState.newTable()
            permissions.forEachIndexed { index, permission ->
                luaState.push(permission)
                luaState.rawSetI(-2, index + 1)
            }
            return@push 1
        }
        lua.setField(-2, "get_player_permissions")

        // add_group(group_name: string, permissions: table[, weight: number, inherit: table): boolean - 添加权限组
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            if (!luaState.isTable(2)) {
                luaState.push(false)
                return@push 1
            }

            val permissions = mutableListOf<String>()
            var weight = 0
            val inherit = mutableListOf<String>()

            // 解析权限列表
            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val permission = luaState.toString(-1)
                if (permission != null) {
                    permissions.add(permission)
                }
                luaState.pop(1)
            }

            // 解析权重
            if (luaState.top > 2 && luaState.isNumber(3)) {
                weight = luaState.toInteger(3).toInt()
            }

            // 解析继承组
            if (luaState.top > 3 && luaState.isTable(4)) {
                luaState.pushNil()
                while (luaState.next(4) != 0) {
                    val group = luaState.toString(-1)
                    if (group != null) {
                        inherit.add(group)
                    }
                    luaState.pop(1)
                }
            }

            luaState.push(permissionManager.addGroup(groupName, permissions, weight, inherit))
            return@push 1
        }
        lua.setField(-2, "add_group")

        // remove_group(group_name: string): boolean - 删除权限组
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(false)
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            luaState.push(permissionManager.removeGroup(groupName))
            return@push 1
        }
        lua.setField(-2, "remove_group")

        // get_group_info(group_name: string): table - 获取权限组信息
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            val config = permissionManager.getConfig()
            if (config == null) {
                luaState.pushNil()
                return@push 1
            }

            if (!config.contains("groups.$groupName")) {
                luaState.pushNil()
                return@push 1
            }

            luaState.newTable()
            luaState.push(groupName)
            luaState.setField(-2, "name")
            luaState.push(config.getInt("groups.$groupName.weight", 0).toLong())
            luaState.setField(-2, "weight")

            val permissions = config.getStringList("groups.$groupName.permissions")
            luaState.newTable()
            permissions.forEachIndexed { index, permission ->
                luaState.push(permission)
                luaState.rawSetI(-2, index + 1)
            }
            luaState.setField(-2, "permissions")

            val inherit = config.getStringList("groups.$groupName.inherit")
            luaState.newTable()
            inherit.forEachIndexed { index, group ->
                luaState.push(group)
                luaState.rawSetI(-2, index + 1)
            }
            luaState.setField(-2, "inherit")

            return@push 1
        }
        lua.setField(-2, "get_group_info")

        // set_group_weight(group_name: string, weight: number): boolean - 设置权限组权重
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            val weight = luaState.toInteger(2).toInt()

            luaState.push(permissionManager.setGroupWeight(groupName, weight))
            return@push 1
        }
        lua.setField(-2, "set_group_weight")

        // get_group_weight(group_name: string): number - 获取权限组权重
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.push(0)
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            luaState.push(permissionManager.getGroupWeight(groupName).toLong())
            return@push 1
        }
        lua.setField(-2, "get_group_weight")

        // set_group_inherit(group_name: string, inherit: table): boolean - 设置权限组继承关系
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val groupName = luaState.toString(1) ?: ""
            if (!luaState.isTable(2)) {
                luaState.push(false)
                return@push 1
            }

            val inherit = mutableListOf<String>()

            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val group = luaState.toString(-1)
                if (group != null) {
                    inherit.add(group)
                }
                luaState.pop(1)
            }

            luaState.push(permissionManager.setGroupInheritance(groupName, inherit))
            return@push 1
        }
        lua.setField(-2, "set_group_inherit")

        // add_player_group(player_name: string, group_name: string): boolean - 将玩家添加到权限组
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val groupName = luaState.toString(2) ?: ""

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            luaState.push(permissionManager.addPlayerToGroup(player, groupName))
            return@push 1
        }
        lua.setField(-2, "add_player_group")

        // remove_player_group(player_name: string, group_name: string): boolean - 将玩家从权限组移除
        lua.push { luaState ->
            if (luaState.top < 2) {
                luaState.push(false)
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val groupName = luaState.toString(2) ?: ""

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            luaState.push(permissionManager.removePlayerFromGroup(player, groupName))
            return@push 1
        }
        lua.setField(-2, "remove_player_group")

        // get_player_groups(player_name: string): table - 获取玩家所属的所有权限组
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val playerName = luaState.toString(1) ?: ""
            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.pushNil()
                return@push 1
            }

            val config = permissionManager.getConfig()
            if (config == null) {
                luaState.pushNil()
                return@push 1
            }

            val uuid = player.uniqueId.toString()
            val path = "players.$uuid.groups"

            if (!config.contains(path)) {
                luaState.pushNil()
                return@push 1
            }

            val groups = config.getStringList(path)
            luaState.newTable()
            groups.forEachIndexed { index, group ->
                luaState.push(group)
                luaState.rawSetI(-2, index + 1)
            }
            return@push 1
        }
        lua.setField(-2, "get_player_groups")

        lua.setGlobal("perm")

        if (!apiNames.contains("perm")) {
            apiNames.add("perm")
        }
    }

    override fun getAPINames(): List<String> = apiNames
} 