package tech.ezeny.luagin.i18n

import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.utils.PLog
import java.text.MessageFormat

class I18n(private val yamlManager: YamlManager) {
    // 当前语言
    private var currentLocale = "en_US"
    // 语言翻译配置
    private var langConfig: YamlConfiguration? = null
    // 默认语言翻译配置
    private var defaultLangConfig: YamlConfiguration? = null
    // 语言设置配置
    private var languageSettingsConfig: YamlConfiguration? = null

    private val langDir = "lang"

    init {
        // 加载 language.yml
        val languageSettingsPath = "$langDir/language.yml"
        languageSettingsConfig = yamlManager.getConfig(languageSettingsPath)
        currentLocale = if (languageSettingsConfig != null) {
            languageSettingsConfig?.getString("Language", "en_US") ?: "en_US"
        } else {
            "en_US"
        }

        // 加载语言翻译文件
        loadLanguage(currentLocale)
    }

    /**
     * 获取本地化字符串
     *
     * @param key 语言键
     * @param args 格式化参数
     * @return 本地化后的字符串，如果找不到则返回键本身
     */
    fun get(key: String, vararg args: Any): String {
        var value = langConfig?.getString(key)

        // 如果当前语言文件没有，尝试从默认语言文件获取
        if (value == null && defaultLangConfig != null) {
            value = defaultLangConfig?.getString(key)
        }

        // 如果仍然找不到，返回键本身
        if (value == null) {
            PLog.warning("i18n.missing_key", key, currentLocale)
            return key
        }

        // 格式化字符串
        return try {
            if (args.isNotEmpty()) {
                MessageFormat.format(value, *args)
            } else {
                value
            }
        } catch (e: IllegalArgumentException) {
            PLog.warning("i18n.format_error", key, e.message ?: "Unknown error")
            value
        }
    }

    /**
     * 加载指定的语言文件
     *
     * @param locale 语言代码 (例如 "en_US")
     */
    private fun loadLanguage(locale: String) {
        val langPath = "$langDir/$locale.yml"
        langConfig = yamlManager.getConfig(langPath)

        if (locale != "en_US") {
            val defaultLangPath = "$langDir/en_US.yml"
            defaultLangConfig = yamlManager.getConfig(defaultLangPath)
        } else {
            defaultLangConfig = null
        }

        // 如果指定的语言文件加载失败，加载默认语言
        if (langConfig == null) {
            currentLocale = "en_US"
            val defaultLangPath = "$langDir/en_US.yml"
            langConfig = yamlManager.getConfig(defaultLangPath)
            defaultLangConfig = null
        }
    }
}