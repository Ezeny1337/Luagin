package tech.ezeny.luagin.config

import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.FileUtils
import tech.ezeny.luagin.utils.LanguageUtils
import tech.ezeny.luagin.utils.PLog
import java.io.File
import java.io.IOException

class YamlManager(plugin: Luagin) {
    private val configCache = mutableMapOf<String, YamlConfiguration>()
    private val configFiles = mutableMapOf<String, File>()

    init {
        // 初始化 FileUtils
        FileUtils.initialize(plugin)

        // 插件配置目录
        val configsFolder = FileUtils.getFile("configs")
        if (!configsFolder.exists()) {
            FileUtils.createDirectory("configs")
        }
        
        // 插件语言目录
        val langFolder = FileUtils.getFile("lang")
        if (!langFolder.exists()) {
            FileUtils.createDirectory("lang")
        }

        LanguageUtils.createDefaultLanguageFiles(langFolder)
    }

    /**
     * 获取或加载 YAML 文件配置
     *
     * @param relativePath 文件相对路径
     * @return YamlConfiguration 实例，文件不存在或加载失败返回 null
     */
    fun getConfig(relativePath: String): YamlConfiguration? {
        val normalizedPath = if (!relativePath.endsWith(".yml")) {
            "$relativePath.yml"
        } else {
            relativePath
        }

        if (configCache.containsKey(normalizedPath)) {
            return configCache[normalizedPath]
        }

        val file = FileUtils.getFile(normalizedPath)

        if (!file.exists()) {
            PLog.warning("config.not_found", normalizedPath)
            return null
        }

        return try {
            val config = YamlConfiguration.loadConfiguration(file)
            configCache[normalizedPath] = config
            configFiles[normalizedPath] = file
            config
        } catch (e: Exception) {
            PLog.warning("config.load_failed", normalizedPath, e.message ?: "Unknown error")
            null
        }
    }

    /**
     * 保存指定相对路径的配置文件
     *
     * @param relativePath 文件相对路径
     * @return 是否保存成功
     */
    fun saveConfig(relativePath: String): Boolean {
        val normalizedPath = if (!relativePath.endsWith(".yml")) {
            "$relativePath.yml"
        } else {
            relativePath
        }

        val config = configCache[normalizedPath]
        val file = configFiles[normalizedPath] ?: FileUtils.getFile(normalizedPath)

        if (config == null) {
            return false
        }

        return try {
            // 确保目录存在
            file.parentFile?.mkdirs()
            config.save(file)
            true
        } catch (e: IOException) {
            PLog.warning("config.save_failed", normalizedPath, e.message ?: "Unknown error")
            false
        }
    }

    /**
     * 创建或更新配置文件
     *
     * @param relativePath 文件相对路径
     * @param configData 配置数据
     * @return 是否保存成功
     */
    fun createOrUpdateConfig(relativePath: String, configData: Map<String, Any>): Boolean {
        val normalizedPath = if (!relativePath.endsWith(".yml")) {
            "$relativePath.yml"
        } else {
            relativePath
        }

        val file = FileUtils.getFile(normalizedPath)
        
        return try {
            // 确保目录存在
            file.parentFile?.mkdirs()
            
            val config = YamlConfiguration()
            configData.forEach { (key, value) ->
                config.set(key, value)
            }
            
            config.save(file)
            configCache[normalizedPath] = config
            configFiles[normalizedPath] = file
            true
        } catch (e: IOException) {
            PLog.warning("config.save_failed", normalizedPath, e.message ?: "Unknown error")
            false
        }
    }
}