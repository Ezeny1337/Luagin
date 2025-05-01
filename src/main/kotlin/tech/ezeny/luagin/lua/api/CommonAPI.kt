package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.utils.PLog

object CommonAPI : LuaAPIProvider, KoinComponent {
    private val permissionManager: PermissionManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(globals: Globals) {
        // 创建 common 表
        val commonTable = LuaTable()
        globals.set("common", commonTable)

        // 检查权限
        commonTable.set("has_permission", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val playerName = args.checkjstring(1)
                val permission = args.checkjstring(2)

                val player = Bukkit.getPlayer(playerName) ?: return valueOf(false)
                return valueOf(player.hasPermission(permission))
            }
        })

        // 添加权限
        commonTable.set("add_permission", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val playerName = args.checkjstring(1)
                val permission = args.checkjstring(2)

                val player = Bukkit.getPlayer(playerName) ?: return valueOf(false)
                return valueOf(permissionManager.addPermission(player, permission))
            }
        })

        // 移除权限
        commonTable.set("remove_permission", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val playerName = args.checkjstring(1)
                val permission = args.checkjstring(2)

                val player = Bukkit.getPlayer(playerName) ?: return valueOf(false)
                return valueOf(permissionManager.removePermission(player, permission))
            }
        })

        // 获取玩家所有权限
        commonTable.set("get_player_permissions", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val playerName = args.checkjstring(1)
                val player = Bukkit.getPlayer(playerName) ?: return NIL

                val config = permissionManager.getConfig() ?: return NIL
                val uuid = player.uniqueId.toString()
                val path = "players.$uuid.permissions"
                
                if (!config.contains(path)) return NIL
                
                val permissions = config.getStringList(path)
                val result = LuaTable()
                permissions.forEachIndexed { index, permission ->
                    result.set(index + 1, valueOf(permission))
                }
                return result
            }
        })

        // 添加权限组
        commonTable.set("add_group", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).istable()) {
                    return valueOf(false)
                }

                val groupName = args.checkjstring(1)
                val permissionsTable = args.checktable(2)
                val permissions = mutableListOf<String>()
                var weight = 0
                val inherit = mutableListOf<String>()

                // 解析权限列表
                var i = 1
                while (true) {
                    val permission = permissionsTable.get(i)
                    if (permission.isnil()) break
                    if (permission.isstring()) {
                        permissions.add(permission.tojstring())
                    }
                    i++
                }

                // 解析权重
                if (args.narg() > 2 && args.arg(3).isnumber()) {
                    weight = args.arg(3).toint()
                }

                // 解析继承组
                if (args.narg() > 3 && args.arg(4).istable()) {
                    val inheritTable = args.checktable(4)
                    i = 1
                    while (true) {
                        val group = inheritTable.get(i)
                        if (group.isnil()) break
                        if (group.isstring()) {
                            inherit.add(group.tojstring())
                        }
                        i++
                    }
                }

                return valueOf(permissionManager.addGroup(groupName, permissions, weight, inherit))
            }
        })

        // 获取权限组信息
        commonTable.set("get_group_info", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val groupName = args.checkjstring(1)
                val config = permissionManager.getConfig() ?: return NIL

                if (!config.contains("groups.$groupName")) return NIL

                val result = LuaTable()
                result.set("name", valueOf(groupName))
                result.set("weight", valueOf(config.getInt("groups.$groupName.weight", 0)))
                
                val permissions = config.getStringList("groups.$groupName.permissions")
                val permissionsTable = LuaTable()
                permissions.forEachIndexed { index, permission ->
                    permissionsTable.set(index + 1, valueOf(permission))
                }
                result.set("permissions", permissionsTable)

                val inherit = config.getStringList("groups.$groupName.inherit")
                val inheritTable = LuaTable()
                inherit.forEachIndexed { index, group ->
                    inheritTable.set(index + 1, valueOf(group))
                }
                result.set("inherit", inheritTable)

                return result
            }
        })

        // 获取权限组权重
        commonTable.set("get_group_weight", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return valueOf(0)
                }

                val groupName = args.checkjstring(1)
                return valueOf(permissionManager.getGroupWeight(groupName))
            }
        })

        // 设置权限组权重
        commonTable.set("set_group_weight", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isnumber()) {
                    return valueOf(false)
                }

                val groupName = args.checkjstring(1)
                val weight = args.arg(2).toint()

                return valueOf(permissionManager.setGroupWeight(groupName, weight))
            }
        })

        // 设置权限组继承关系
        commonTable.set("set_group_inherit", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).istable()) {
                    return valueOf(false)
                }

                val groupName = args.checkjstring(1)
                val inheritTable = args.checktable(2)
                val inherit = mutableListOf<String>()

                var i = 1
                while (true) {
                    val group = inheritTable.get(i)
                    if (group.isnil()) break
                    if (group.isstring()) {
                        inherit.add(group.tojstring())
                    }
                    i++
                }

                return valueOf(permissionManager.setGroupInheritance(groupName, inherit))
            }
        })

        // 将玩家添加到权限组
        commonTable.set("add_player_group", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val playerName = args.checkjstring(1)
                val groupName = args.checkjstring(2)

                val player = Bukkit.getPlayer(playerName) ?: return valueOf(false)
                return valueOf(permissionManager.addPlayerToGroup(player, groupName))
            }
        })

        // 将玩家从权限组移除
        commonTable.set("remove_player_group", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return valueOf(false)
                }

                val playerName = args.checkjstring(1)
                val groupName = args.checkjstring(2)

                val player = Bukkit.getPlayer(playerName) ?: return valueOf(false)
                return valueOf(permissionManager.removePlayerFromGroup(player, groupName))
            }
        })

        // 获取玩家所属的所有权限组
        commonTable.set("get_player_groups", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return NIL
                }

                val playerName = args.checkjstring(1)
                val player = Bukkit.getPlayer(playerName) ?: return NIL

                val config = permissionManager.getConfig() ?: return NIL
                val uuid = player.uniqueId.toString()
                val path = "players.$uuid.groups"
                
                if (!config.contains(path)) return NIL
                
                val groups = config.getStringList(path)
                val result = LuaTable()
                groups.forEachIndexed { index, group ->
                    result.set(index + 1, valueOf(group))
                }
                return result
            }
        })

        // 添加到 API 名称列表
        if (!apiNames.contains("common")) {
            apiNames.add("common")
        }

        PLog.info("log.info.common_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
} 