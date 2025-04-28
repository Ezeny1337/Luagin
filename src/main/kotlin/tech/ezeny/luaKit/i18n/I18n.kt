package tech.ezeny.luaKit.i18n

import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import tech.ezeny.luaKit.config.YamlManager
import tech.ezeny.luaKit.config.YamlManager.langFolder
import tech.ezeny.luaKit.utils.PLog
import java.io.File
import java.text.MessageFormat

object I18n {
    private lateinit var plugin: JavaPlugin

    // 当前语言
    private var currentLocale = "en_US"

    // 语言翻译配置
    private var langConfig: YamlConfiguration? = null

    // 默认语言翻译配置
    private var defaultLangConfig: YamlConfiguration? = null

    // 语言设置配置
    private var languageSettingsConfig: YamlConfiguration? = null

    fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin

        // 加载 language.yml
        val languageConfigFile = File(langFolder, "language.yml")
        if (languageConfigFile.exists()) {
            languageSettingsConfig = YamlManager.loadConfig("language", langFolder)
            currentLocale = languageSettingsConfig?.getString("Language", "en_US") ?: "en_US"
        } else {
            currentLocale = "en_US"
        }

        // 加载语言翻译文件
        loadLanguage(currentLocale)

        PLog.info("i18n.initialized", currentLocale)
    }

    /**
     * 加载指定语言的翻译文件
     * 如果不是英语，则加载英语作为回退选项
     * @param locale 语言代码 (如 "en_US", "zh_CN")
     */
    private fun loadLanguage(locale: String) {
        langConfig = YamlManager.loadConfig(locale, langFolder)

        if (locale != "en_US") {
            defaultLangConfig = YamlManager.loadConfig("en_US", langFolder)
        }

        if (langConfig == null) {
            PLog.warning("i18n.language_not_found", locale)
            currentLocale = "en_US"
            langConfig = YamlManager.loadConfig("en_US", langFolder)
        }
    }

    /**
     * 获取本地化文本
     * @param key 文本键
     * @param args 格式化参数
     * @return 本地化后的文本
     */
    fun get(key: String, vararg args: Any): String {
        var message = langConfig?.getString(key)

        if (message == null && defaultLangConfig != null) {
            message = defaultLangConfig?.getString(key)
        }

        if (message == null) {
            return key
        }

        return if (args.isNotEmpty()) {
            MessageFormat.format(message, *args)
        } else {
            message
        }
    }
}