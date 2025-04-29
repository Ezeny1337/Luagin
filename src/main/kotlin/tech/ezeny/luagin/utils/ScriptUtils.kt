package tech.ezeny.luagin.utils

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import tech.ezeny.luagin.events.EventManager
import kotlin.getValue

object ScriptUtils : KoinComponent {
    private val eventManager: EventManager by inject()

    // 获取当前正在加载的脚本名称
    fun getCurrentScript(): String? {
        return eventManager.getCurrentScript()
    }

    // 设置当前正在加载的脚本名称
    fun setCurrentScript(scriptName: String) {
        eventManager.setCurrentScript(scriptName)
    }
}