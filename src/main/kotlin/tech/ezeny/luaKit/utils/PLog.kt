package tech.ezeny.luaKit.utils

import tech.ezeny.luaKit.i18n.I18n
import java.util.logging.Level
import java.util.logging.Logger

object PLog {

    private var loggerInstance: Logger? = null

    fun initialize(logger: Logger) {
        if (loggerInstance == null) {
            loggerInstance = logger
        }
    }

    /**
     * 内部日志记录方法
     */
    private fun log(level: Level, message: String, thrown: Throwable? = null) {
        if (loggerInstance == null) {
            thrown?.printStackTrace()
            return
        }

        // 获取本地化前缀
        val prefix = when (level) {
            Level.INFO -> I18n.get("log.info.prefix")
            Level.WARNING -> I18n.get("log.warning.prefix")
            Level.SEVERE -> I18n.get("log.severe.prefix")
            else -> ""
        }

        loggerInstance?.log(level, prefix + message, thrown)
    }

    /**
     * 记录信息级别日志
     */
    fun info(key: String, vararg args: Any) {
        log(Level.INFO, I18n.get(key, *args))
    }

    /**
     * 记录警告级别日志
     */
    fun warning(key: String, vararg args: Any) {
        log(Level.WARNING, I18n.get(key, *args))
    }

    /**
     * 记录严重级别日志
     */
    fun severe(key: String, vararg args: Any) {
        log(Level.SEVERE, I18n.get(key, *args))
    }
}