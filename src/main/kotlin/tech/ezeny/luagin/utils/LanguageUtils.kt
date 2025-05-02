package tech.ezeny.luagin.utils

import org.bukkit.configuration.file.YamlConfiguration
import java.io.File

object LanguageUtils {

    /**
     * 创建默认语言文件
     * @param configFolder 配置文件夹
     */
    fun createDefaultLanguageFiles(configFolder: File) {

        val langFile = File(configFolder, "language.yml")
        if (!langFile.exists()) {
            val langConfig = YamlConfiguration()
            langConfig.set("Language", "en_US")
            langConfig.save(langFile)
        }

        // 创建英文语言文件
        val enFile = File(configFolder, "en_US.yml")
        if (!enFile.exists()) {
            val enConfig = YamlConfiguration()

            // 日志前缀
            enConfig.set("log.info.prefix", "[INFO] ")
            enConfig.set("log.warning.prefix", "[WARNING] ")
            enConfig.set("log.severe.prefix", "[SEVERE] ")

            // 信息级别消息
            enConfig.set("log.info.loading", "Loading...")
            enConfig.set("log.info.loading_completed", "Loading completed")
            enConfig.set("log.info.unloading", "Unloading...")
            enConfig.set("log.info.unloading_completed", "Unloading completed")
            enConfig.set("log.info.clear_handlers_for_script", "All event handlers for script {0} have been removed")
            enConfig.set("log.info.register_event_handler", "Registered event handler for {0} (from script {1})")
            enConfig.set("log.info.register_bukkit_listener", "Registered Bukkit listener for {0}")
            enConfig.set("log.info.clear_handlers", "All Lua event handlers removed and Bukkit listeners unregistered")
            enConfig.set("log.info.set_environment", "Lua main environment has been set")
            enConfig.set("log.info.lua_loading_completed", "All Lua scripts loaded successfully, {0} scripts in total")
            enConfig.set("log.info.loading_lua_succeeded", "Lua loaded successfully: {0}")
            enConfig.set("log.info.mysql_connected","Connected to MySQL")
            enConfig.set("log.info.events_api_set", "Events API has been set")
            enConfig.set("log.info.chat_api_set", "Chat API has been set")
            enConfig.set("log.info.title_api_set", "Title API has been set")
            enConfig.set("log.info.actionbar_api_set", "ActionBar API has been set")
            enConfig.set("log.info.utils_api_set", "Utils API has been set")
            enConfig.set("log.info.files_api_set", "Files API has been set")
            enConfig.set("log.info.yaml_api_set", "YAML API has been set")
            enConfig.set("log.info.comm_api_set","Comm API has been set")
            enConfig.set("log.info.mysql_api_set","MySQL API has been set")
            enConfig.set("log.info.command_api_set","Command API has been set")
            enConfig.set("log.info.common","Common API has been set")

            // 警告级别消息
            enConfig.set("log.warning.event_not_found", "Event class {0}.{1} not found")
            enConfig.set("log.warning.register_event_handler_failed", "Failed to register event handler: {0}")
            enConfig.set("log.warning.unset_event_handler_failed", "Failed to unregister event handler: {0}")
            enConfig.set("log.warning.handle_event_failed", "Failed to execute Lua event handler (from script {0}): {1}")
            enConfig.set("log.warning.scripts_not_found", "No Lua scripts found in {0}")
            enConfig.set("log.warning.script_not_found", "Lua script not found: {0}")
            enConfig.set("log.warning.invalid_color", "Invalid color code: {0}")
            enConfig.set("log.warning.getter_failed", "Failed to find or invoking getter {0} or {1}: {2}")
            enConfig.set("log.warning.set_property_failed", "Failed to set property {0}: {1}")
            enConfig.set("log.warning.set_property_failed_not_found", "Cannot set property {0}: suitable setter method not found")
            enConfig.set("log.warning.player_not_found", "Player not found: {0}")
            enConfig.set("log.warning.command_exec_failed", "Failed to execute command {0}: {1}")
            enConfig.set("log.warning.execute_after_failed", "Failed to execute delayed callback: {0}")
            enConfig.set("log.warning.mkdirs_failed", "Failed to create directory {0}: {1}")
            enConfig.set("log.warning.create_file_failed", "Failed to create file {0}: {1}")
            enConfig.set("log.warning.dir_not_found", "Directory not found: {0}")
            enConfig.set("log.warning.function_not_found","{0} script cannot find the function: {1}")
            enConfig.set("log.warning.function_call_failed","Failed to call {0} script`s {1} function: {2}")
            enConfig.set("log.warning.read_file_failed","Failed to read file {0}: {1}")
            enConfig.set("log.warning.write_file_failed","Failed to write file {0}: {1}")
            enConfig.set("log.warning.mysql_create_table_failed","Failed to create MySQL table {0}: {1}")
            enConfig.set("log.warning.mysql_insert_failed","Failed to insert MySQL table {0}: {1}")
            enConfig.set("log.warning.mysql_update_failed","Failed to update MySQL table {0}: {1}")
            enConfig.set("log.warning.mysql_query_failed","Failed to query MySQL table {0}: {1}")
            enConfig.set("log.warning.execute_command_failed","Failed to execute command /{0}: {1}")

            // 严重级别消息
            enConfig.set("log.severe.copy_shared_api_failed", "Shared API: {0} is nil in the main environment")
            enConfig.set("log.severe.lua_dir_not_found", "Script directory does not exist: {0}")
            enConfig.set("log.severe.lua_not_found", "Script file not found: {0}")
            enConfig.set("log.severe.loading_lua_failed", "Failed to load {0}: {1}")
            enConfig.set("log.severe.mysql_connection_failed","Failed to connect MySQL: {0}")
            enConfig.set("log.severe.register_permission_failed","Failed to register permission {0}: {1}")

            // 配置文件消息
            enConfig.set("config.not_found", "Config not found: {0}")
            enConfig.set("config.load_failed", "Failed to load config file {0}: {1}")
            enConfig.set("config.save_failed", "Failed to save config file {0}: {1}")

            // 命令消息
            enConfig.set("command.help.title", "=== Luagin Command Help ===")
            enConfig.set("command.help.reload_all", "/luagin reload - Reload all Lua scripts")
            enConfig.set("command.help.reload_specific", "/luagin reload <script> - Reload specific Lua script")
            enConfig.set("command.help.help", "/luagin help - Show this help message")
            enConfig.set("command.check_help", "Use /luagin help to see available commands")
            enConfig.set("command.reload.script.loading", "Reloading script: {0}")
            enConfig.set("command.reload.script.success", "Script {0} has been successfully reloaded")
            enConfig.set(
                "command.reload.script.failure",
                "Unable to reload script {0}. Please check if the script exists"
            )
            enConfig.set("command.reload.all.loading", "Reloading all Lua scripts...")
            enConfig.set("command.reload.all.success", "Successfully reloaded {0} scripts")
            enConfig.set("command.no_permission","Permission denied!")
            enConfig.set("command.missing_argument","Missing argument: {0}")
            enConfig.set("command.invalid_argument","Invalid argument: {0}")

            // I18n 消息
            enConfig.set("i18n.format_failed", "I18n format failed: {0}")

            enConfig.save(enFile)
        }

        // 创建中文语言文件
        val zhFile = File(configFolder, "zh_CN.yml")
        if (!zhFile.exists()) {
            val zhConfig = YamlConfiguration()

            // 日志前缀
            zhConfig.set("log.info.prefix", "[信息] ")
            zhConfig.set("log.warning.prefix", "[警告] ")
            zhConfig.set("log.severe.prefix", "[严重] ")

            // 信息级别消息
            zhConfig.set("log.info.loading_completed", "加载完成")
            zhConfig.set("log.info.unloading_completed", "卸载完成")
            zhConfig.set("log.info.clear_handlers_for_script","已移除脚本 {0} 的所有事件处理器")
            zhConfig.set("log.info.register_event_handler","已为 {0} 注册事件处理器 (来自脚本 {1})")
            zhConfig.set("log.info.register_bukkit_listener","已为 {0} 注册 Bukkit 监听器")
            zhConfig.set("log.info.clear_handlers","已移除所有 Lua 事件处理器并取消注册 Bukkit 监听器")
            zhConfig.set("log.info.set_environment","Lua 主环境已设置")
            zhConfig.set("log.info.lua_loading_completed","所有 Lua 脚本加载完成, 共加载 {0} 个脚本")
            zhConfig.set("log.info.loading_lua_succeeded","成功加载 Lua: {0}")
            zhConfig.set("log.info.mysql_connected","已连接到 MySQL")
            zhConfig.set("log.info.events_api_set","Events API 已设置")
            zhConfig.set("log.info.chat_api_set","Chat API 已设置")
            zhConfig.set("log.info.title_api_set","Title API 已设置")
            zhConfig.set("log.info.actionbar_api_set","ActionBar API 已设置")
            zhConfig.set("log.info.utils_api_set","Utils API 已设置")
            zhConfig.set("log.info.files_api_set","Files API 已设置")
            zhConfig.set("log.info.yaml_api_set","YAML API 已设置")
            zhConfig.set("log.info.comm_api_set","Comm API 已设置")
            zhConfig.set("log.info.mysql_api_set","MySQL API 已设置")
            zhConfig.set("log.info.command_api_set","Command API 已设置")
            zhConfig.set("log.info.common_api_set","Common API 已设置")

            zhConfig.set("log.warning.event_not_found","找不到事件类 {0}.{1}")
            zhConfig.set("log.warning.register_event_handler_failed","注册事件处理器失败: {0}")
            zhConfig.set("log.warning.unset_event_handler_failed","取消注册事件处理器失败: {0}")
            zhConfig.set("log.warning.handle_event_failed","执行 Lua 事件处理器失败 (来自脚本 {0}): {1}")
            zhConfig.set("log.warning.scripts_not_found","在 {0} 未找到任何 Lua 脚本")
            zhConfig.set("log.warning.script_not_found","Lua 脚本不存在: {0}")
            zhConfig.set("log.warning.invalid_color","无效的颜色代码: {0}")
            zhConfig.set("log.warning.getter_failed","查找或调用 getter {0} 或 {1} 失败: {2}")
            zhConfig.set("log.warning.set_property_failed","设置属性 {0} 失败: {1}")
            zhConfig.set("log.warning.set_property_failed_not_found","无法设置属性 {0}: 找不到合适的 setter 方法")
            zhConfig.set("log.warning.player_not_found","找不到玩家: {0}")
            zhConfig.set("log.warning.command_exec_failed","执行命令 {0} 失败: {1}")
            zhConfig.set("log.warning.execute_after_failed","执行延迟回调失败: {0}")
            zhConfig.set("log.warning.mkdirs_failed","创建目录 {0} 失败: {1}")
            zhConfig.set("log.warning.create_file_failed","创建文件 {0} 失败: {1}")
            zhConfig.set("log.warning.dir_not_found","目录不存在: {0}")
            zhConfig.set("log.warning.function_not_found","{0} 脚本找不到函数: {1}")
            zhConfig.set("log.warning.function_call_failed","{0} 脚本的 {1} 函数调用失败: {2}")
            zhConfig.set("log.warning.read_file_failed","读取文件 {0} 失败: {1}")
            zhConfig.set("log.warning.write_file_failed","写入文件 {0} 失败: {1}")
            zhConfig.set("log.warning.mysql_create_table_failed","创建 MySQL 表 {0} 失败: {1}")
            zhConfig.set("log.warning.mysql_insert_failed","插入 MySQL 表 {0} 失败: {1}")
            zhConfig.set("log.warning.mysql_update_failed","更新 MySQL 表 {0} 失败: {1}")
            zhConfig.set("log.warning.mysql_query_failed","查询 MySQL 表 {0} 失败: {1}")
            zhConfig.set("log.warning.execute_command_failed","执行命令 /{0} 失败: {1}")

            // 严重级别消息
            zhConfig.set("log.severe.copy_shared_api_failed","共享 API: {0} 在主环境为 nil")
            zhConfig.set("log.severe.lua_dir_not_found","脚本目录不存在: {0}")
            zhConfig.set("log.severe.lua_not_found","脚本文件不存在: {0}")
            zhConfig.set("log.severe.loading_lua_failed","加载 {0} 失败: {1}")
            zhConfig.set("log.severe.mysql_connection_failed","连接 MySQL 失败: {0}")
            zhConfig.set("log.severe.register_permission_failed","注册权限 {0} 失败: {1}")

            // 配置文件消息
            zhConfig.set("config.not_found", "配置文件不存在: {0}")
            zhConfig.set("config.load_failed", "加载配置文件 {0} 失败: {1}")
            zhConfig.set("config.save_failed", "保存配置文件 {0} 失败: {1}")

            // 命令消息
            zhConfig.set("command.help.title", "=== Luagin 命令帮助 ===")
            zhConfig.set("command.help.reload_all", "/luagin reload - 重新加载所有 Lua 脚本")
            zhConfig.set("command.help.reload_specific", "/luagin reload <脚本名> - 重新加载指定 Lua 脚本")
            zhConfig.set("command.help.help", "/luagin help - 显示此帮助信息")
            zhConfig.set("command.check_help", "使用 /luagin help 查看帮助")
            zhConfig.set("command.reload.script.loading", "正在重新加载脚本: {0}")
            zhConfig.set("command.reload.script.success", "脚本 {0} 已成功重新加载")
            zhConfig.set("command.reload.script.failure", "无法重新加载脚本 {0}, 请检查脚本是否存在")
            zhConfig.set("command.reload.all.loading", "正在重新加载所有 Lua 脚本...")
            zhConfig.set("command.reload.all.success", "成功重新加载了 {0} 个脚本")

            // I18n 消息
            zhConfig.set("i18n.format_failed", "I18n 格式化失败: {0}")

            zhConfig.save(zhFile)
        }
    }
}