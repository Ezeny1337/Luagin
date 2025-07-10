package tech.ezeny.luagin.commands

import org.bukkit.Bukkit
import org.bukkit.command.CommandMap
import org.bukkit.command.CommandSender
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.permissions.PermissionManager
import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

class CommandManager(private val plugin: Luagin, private val permissionManager: PermissionManager) {
    // 存储所有注册的命令
    private val registeredCommands = ConcurrentHashMap<String, LuaCommand>()

    // 存储命令的参数提示，结构为：命令名 - 位置 - 前置参数 - 参数信息（包含列表和权限）
    private val commandTabCompletions =
        ConcurrentHashMap<String, ConcurrentHashMap<Int, ConcurrentHashMap<String, ArgInfo>>>()

    // 参数信息类，包含参数列表和权限
    data class ArgInfo(
        val args: List<String>,
        val permission: String = ""  // 表示使用上级权限
    )

    // Bukkit 命令映射
    private val commandMap: CommandMap

    init {
        // 获取 Bukkit 的命令映射
        val commandMapField: Field = Bukkit.getServer().javaClass.getDeclaredField("commandMap")
        commandMapField.isAccessible = true
        commandMap = commandMapField.get(Bukkit.getServer()) as CommandMap
    }

    /**
     * 注册一个新命令
     */
    fun registerCommand(name: String, permission: String, lua: Lua, handlerRef: Int): LuaCommand {
        // 创建命令对象
        val command = LuaCommand(name, permission, lua, handlerRef, this, permissionManager)

        // 注册到 Bukkit
        commandMap.register(plugin.name.lowercase(), command)

        // 存储命令
        registeredCommands[name] = command

        return command
    }

    /**
     * 添加命令参数的 Tab 补全（不覆盖现有参数）
     *
     * @param commandName 命令名
     * @param position 参数位置
     * @param arguments 补全参数列表
     * @param previousArg 前一个位置的参数值，如果是位置1则为空字符串
     * @param permission 参数权限，如果为空则使用上级权限
     */
    fun addCommandArguments(
        commandName: String,
        position: Int,
        arguments: List<String>,
        previousArg: String = "",
        permission: String = ""
    ) {
        val cmdArgs = commandTabCompletions.computeIfAbsent(commandName) { ConcurrentHashMap() }
        val posArgs = cmdArgs.computeIfAbsent(position) { ConcurrentHashMap() }

        // 获取或创建参数信息
        val key = "$previousArg:$permission"
        val existingArgInfo = posArgs[key]
        val existingArgs = existingArgInfo?.args?.toMutableList() ?: mutableListOf()

        // 添加新参数（避免重复）
        arguments.forEach { arg ->
            if (!existingArgs.contains(arg)) {
                existingArgs.add(arg)
            }
        }

        // 更新参数信息，使用指定的权限
        posArgs[key] = ArgInfo(existingArgs, permission)
    }

    /**
     * 递归查找参数的权限（用于补全和执行）
     *
     * @param command LuaCommand
     * @param position 参数位置
     * @param arg 补全参数
     * @param previousArg 前一个位置的参数值
     */
    private fun resolvePermission(
        command: LuaCommand,
        position: Int,
        arg: String,
        previousArg: String = ""
    ): String {
        val cmdArgs = commandTabCompletions[command.name] ?: return command.getRequiredPermission()
        val posArgs = cmdArgs[position] ?: return command.getRequiredPermission()
        for ((key, argInfo) in posArgs) {
            val parts = key.split(":", limit = 2)
            val keyPrevArg = parts[0]
            val keyPermission = if (parts.size > 1) parts[1] else ""
            if ((keyPrevArg == previousArg || keyPrevArg == "") && argInfo.args.contains(arg)) {
                if (keyPermission.isNotEmpty()) return keyPermission
                // 递归查找上一级参数的权限
                if (position > 1) {
                    val prevArg = if (position > 2) previousArg else ""
                    return resolvePermission(command, position - 1, previousArg, prevArg)
                }
            }
        }
        return command.getRequiredPermission()
    }

    /**
     * 获取命令的 Tab 补全
     *
     * @param commandName 命令名
     * @param position 当前参数位置
     * @param currentArg 当前正在输入的参数
     * @param previousArg 前一个位置的参数值，如果是位置1则为空字符串
     * @param sender 命令发送者
     */
    fun getTabCompletions(
        commandName: String,
        position: Int,
        currentArg: String,
        previousArg: String = "",
        sender: CommandSender? = null
    ): List<String> {
        val cmdArgs = commandTabCompletions[commandName] ?: return emptyList()
        val posArgs = cmdArgs[position] ?: return emptyList()
        val command = registeredCommands[commandName] ?: return emptyList()

        // 获取所有可能的参数
        val allArgs = mutableListOf<String>()

        // 遍历所有参数信息
        for ((key, argInfo) in posArgs) {
            val parts = key.split(":", limit = 2)
            val keyPrevArg = parts[0]
            // 检查前置参数是否匹配
            if (keyPrevArg == previousArg || keyPrevArg == "") {
                // 检查权限（递归继承）
                for (arg in argInfo.args) {
                    val permission = resolvePermission(command, position, arg, previousArg)
                    if (sender == null || permissionManager.hasPermission(sender, permission)) {
                        if (arg.startsWith(currentArg, ignoreCase = true)) {
                            allArgs.add(arg)
                        }
                    }
                }
            }
        }
        return allArgs
    }

    /**
     * 获取已注册的命令
     */
    fun getCommand(name: String): LuaCommand? {
        return registeredCommands[name]
    }

    /**
     * 获取参数的权限
     * @param commandName 命令名
     * @param position 参数位置
     * @param arg 参数值
     * @param previousArg 前一个参数值
     * @return 参数权限，如果未找到则返回空字符串
     */
    fun getArgumentPermission(commandName: String, position: Int, arg: String, previousArg: String = ""): String {
        val cmdArgs = commandTabCompletions[commandName] ?: return ""
        val posArgs = cmdArgs[position] ?: return ""

        // 遍历所有参数信息
        for ((key, argInfo) in posArgs) {
            // 解析键
            val parts = key.split(":", limit = 2)
            val keyPrevArg = parts[0]
            val keyPermission = if (parts.size > 1) parts[1] else ""

            // 检查前置参数和参数值是否匹配
            if ((keyPrevArg == previousArg || keyPrevArg == "") && argInfo.args.contains(arg)) {
                return keyPermission
            }
        }

        return ""
    }
}