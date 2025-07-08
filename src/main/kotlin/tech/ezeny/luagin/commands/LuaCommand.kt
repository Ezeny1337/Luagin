package tech.ezeny.luagin.commands

import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.command.ConsoleCommandSender
import org.bukkit.entity.Player
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.utils.PLog

class LuaCommand(
    name: String,
    private val requiredPermission: String,
    private val lua: Lua,
    private val handlerRef: Int,
    private val commandManager: CommandManager,
    private val permissionManager: PermissionManager
) : Command(name) {

    init {
        permission = requiredPermission
    }

    /**
     * 执行命令的核心逻辑
     *
     * @param sender 执行命令的发送者
     * @param commandLabel 命令的标签
     * @param args 命令参数
     * @return 执行结果
     */
    override fun execute(sender: CommandSender, commandLabel: String, args: Array<String>): Boolean {
        // 检查命令权限
        if (!sender.hasPermission(requiredPermission) && requiredPermission.isNotEmpty()) {
            sender.sendMessage("§cPermission Denied!")
            return true
        }

        // 检查参数权限
        for (i in args.indices) {
            val arg = args[i]
            val previousArg = if (i > 0) args[i - 1] else ""
            val position = i + 1

            // 获取参数权限
            var argPermission = commandManager.getArgumentPermission(name, position, arg, previousArg)

            // 如果参数没有设置权限，则使用上级权限
            if (argPermission.isEmpty()) {
                // 如果是第一个参数，则使用命令权限
                if (i == 0) {
                    argPermission = requiredPermission
                } else {
                    // 否则使用前一个参数的权限
                    val prevArgPermission =
                        commandManager.getArgumentPermission(name, i, previousArg, if (i > 1) args[i - 2] else "")
                    argPermission = prevArgPermission.ifEmpty { requiredPermission }
                }
            }

            // 检查权限
            if (argPermission.isNotEmpty() && !sender.hasPermission(argPermission)) {
                sender.sendMessage("§cPermission Denied!")
                return true
            }
        }

        // 推送 handler 到栈顶
        lua.rawGetI(LUA_REGISTRYINDEX, handlerRef)
        // 推送 sender
        LuaValueFactory.pushJavaObject(lua, sender)
        // 推送 args table
        lua.newTable()
        args.forEachIndexed { index, arg ->
            lua.push((index + 1).toLong())
            lua.push(arg)
            lua.setTable(-3)
        }
        // 调用
        return try {
            lua.pCall(2, 1)
            val result = lua.toBoolean(-1)
            lua.pop(1)
            result
        } catch (e: Exception) {
            PLog.warning("log.warning.execute_command_failed", name, e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 获取命令的自动补全建议
     *
     * @param sender 执行命令的发送者
     * @param alias 命令别名
     * @param args 参数列表
     * @return 补全的建议列表
     */
    override fun tabComplete(sender: CommandSender, alias: String, args: Array<String>): MutableList<String> {
        // 获取当前参数位置的补全
        val position = args.size
        val currentArg = if (args.isNotEmpty()) args[position - 1] else ""

        // 获取前一个参数的值（如果有的话）
        val previousArg = if (position > 1) args[position - 2] else ""

        val completions =
            commandManager.getTabCompletions(name, position, currentArg, previousArg, sender).toMutableList()
        return completions
    }

    /**
     * 检查发送者是否具有权限执行该命令
     *
     * @param target 目标发送者（玩家或控制台）
     * @return 是否有权限
     */
    override fun testPermissionSilent(target: CommandSender): Boolean {
        return when (target) {
            is Player -> {
                permissionManager.hasPermissionFromConfig(target, requiredPermission)  // 直接将 target 转换为 Player
            }

            is ConsoleCommandSender -> true
            else -> false
        }
    }

    /**
     * 添加某个位置的参数补全选项
     *
     * @param position 参数位置
     * @param args 补全选项列表
     * @param permission 参数所需权限，如果为空则使用命令的默认权限
     */
    fun addArgs(position: Int, args: List<String>, permission: String = "") {
        commandManager.addCommandArguments(name, position, args, "", permission)
    }

    /**
     * 添加特定位置的参数提示，并指定前置参数
     *
     * @param position 参数位置
     * @param args 补全参数列表
     * @param previousArg 前一个位置的参数值
     * @param permission 参数权限，如果为空则使用上级权限
     */
    fun addArgsForPrevious(position: Int, args: List<String>, previousArg: String, permission: String = "") {
        commandManager.addCommandArguments(name, position, args, previousArg, permission)
    }

    /**
     * 获取需要的权限
     */
    fun getRequiredPermission(): String {
        return requiredPermission
    }
}