package tech.ezeny.luaKit.config

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import tech.ezeny.luaKit.i18n.LanguageManager
import tech.ezeny.luaKit.utils.PLog
import java.io.File
import java.io.IOException

object YamlManager {
    private lateinit var plugin: JavaPlugin
    private val configCache = mutableMapOf<String, YamlConfiguration>()
    private val configFiles = mutableMapOf<String, File>()

    // 插件配置目录
    lateinit var configFolder: File
        private set

    // 插件语言目录
    lateinit var langFolder: File
        private set

    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin

        // 插件配置目录
        configFolder = File(plugin.dataFolder, "configs")
        if (!configFolder.exists()) {
            configFolder.mkdirs()
            PLog.info("config.folder.created", configFolder.absolutePath)
        }

        // 创建语言文件目录
        langFolder = File(configFolder, "lang")
        if (!langFolder.exists()) {
            langFolder.mkdirs()
            PLog.info("config.folder.created", langFolder.absolutePath)
        }

        // 创建默认语言文件
        LanguageManager.createDefaultLanguageFiles(langFolder)
    }

    /**
     * 加载 YAML 配置文件
     * @param name 配置文件名称 (不含扩展名)
     * @param directory 配置文件目录
     * @return 配置对象
     */
    fun loadConfig(name: String, directory: File): YamlConfiguration? {
        val configFile = File(directory, "$name.yml")
        val configKey = "${directory.path}/$name"

        return try {
            if (!configFile.exists()) {
                PLog.warning("config.file.not_found", configFile.absolutePath)
                return null
            }

            val config = YamlConfiguration.loadConfiguration(configFile)
            configCache[configKey] = config
            configFiles[configKey] = configFile
            config
        } catch (e: Exception) {
            PLog.warning("config.file.load_error", name, e.message ?: "Unknown error")
            null
        }
    }

    /**
     * 获取或加载 YAML 配置对象
     * @param filePath 配置文件完整路径
     * @return 配置对象
     */
    fun getConfigFromPath(filePath: String): YamlConfiguration? {
        val config = configCache[filePath]

        if (config != null) {
            return config
        }

        val configFile = File(filePath)

        return try {
            if (!configFile.exists()) {
                val parentDir = configFile.parentFile
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs()
                }
                configFile.createNewFile()
            }

            val loadedConfig = YamlConfiguration.loadConfiguration(configFile)
            configCache[filePath] = loadedConfig
            configFiles[filePath] = configFile
            loadedConfig
        } catch (e: Exception) {
            PLog.warning("config.file.load_error", configFile.name, e.message ?: "Unknown error")
            null
        }
    }

    /**
     * 保存配置到指定路径
     * @param filePath 配置文件完整路径
     * @return 是否保存成功
     */
    fun saveConfigToPath(filePath: String): Boolean {
        val config = configCache[filePath]
        val configFile = configFiles[filePath]

        if (config == null || configFile == null) {
            PLog.warning("config.not_found", filePath)
            return false
        }

        return try {
            config.save(configFile)
            true
        } catch (e: IOException) {
            PLog.warning("config.file.save_error", configFile.name, e.message ?: "Unknown error")
            false
        }
    }
}