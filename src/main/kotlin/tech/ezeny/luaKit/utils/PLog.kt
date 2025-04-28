package tech.ezeny.luaKit.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.ezeny.luaKit.i18n.I18n
import java.util.logging.Level
import java.util.logging.Logger
import kotlin.getValue

object PLog : KoinComponent {
    private val i18n: I18n by inject()

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
            Level.INFO -> i18n.get("log.info.prefix")
            Level.WARNING -> i18n.get("log.warning.prefix")
            Level.SEVERE -> i18n.get("log.severe.prefix")
            else -> ""
        }

        loggerInstance?.log(level, prefix + message, thrown)
    }

    fun info(key: String, vararg args: Any) {
        log(Level.INFO, i18n.get(key, *args))
    }

    fun warning(key: String, vararg args: Any) {
        log(Level.WARNING, i18n.get(key, *args))
    }

    fun severe(key: String, vararg args: Any) {
        log(Level.SEVERE, i18n.get(key, *args))
    }
}