package tech.ezeny.luagin.events

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import tech.ezeny.luagin.lua.LuaValueFactory

class EventManager(private val plugin: Luagin) : Listener {
    // 存储 Lua 处理函数，按 Event 类分组
    private val luaEventHandlers = mutableMapOf<Class<out Event>, MutableList<Pair<String, LuaFunction>>>()

    // 跟踪已经为哪些 Event 类注册了 Bukkit 监听器
    private val registeredListenerTypes = mutableSetOf<Class<out Event>>()

    // 当前正在加载的脚本文件名
    private var currentScriptName: String = ""

    // 事件分类和对应的 Bukkit 包名
    private val eventCategories = mapOf(
        "block" to "org.bukkit.event.block",
        "enchantment" to "org.bukkit.event.enchantment",
        "entity" to "org.bukkit.event.entity",
        "hanging" to "org.bukkit.event.hanging",
        "inventory" to "org.bukkit.event.inventory",
        "player" to "org.bukkit.event.player",
        "raid" to "org.bukkit.event.raid",
        "server" to "org.bukkit.event.server",
        "vehicle" to "org.bukkit.event.vehicle",
        "weather" to "org.bukkit.event.weather",
        "world" to "org.bukkit.event.world"
    )

    /**
     * 获取当前正在加载的脚本名称
     */
    fun getCurrentScript(): String? {
        return currentScriptName.ifEmpty { null }
    }

    /**
     * 设置当前正在加载的脚本名称
     */
    fun setCurrentScript(scriptName: String) {
        currentScriptName = scriptName
    }

    /**
     * 获取所有事件分类及其对应的包名
     */
    fun getEventCategories(): Map<String, String> = eventCategories

    /**
     * 清除指定脚本的所有事件处理器
     * 会移除脚本相关的 Lua 事件处理函数，并取消注册事件监听器
     */
    fun clearHandlersForScript(scriptName: String) {
        // 遍历所有事件类型
        val eventClassesToRemove = mutableListOf<Class<out Event>>()

        luaEventHandlers.forEach { (eventClass, handlers) ->
            // 过滤并移除属于指定脚本的处理器
            val handlersToRemove = handlers.filter { it.first == scriptName }
            handlers.removeAll(handlersToRemove)

            // 如果某个事件类型没有剩余的处理器，则标记为需要移除
            if (handlers.isEmpty()) {
                eventClassesToRemove.add(eventClass)
            }
        }

        // 移除没有处理器的事件类型
        eventClassesToRemove.forEach { luaEventHandlers.remove(it) }

        PLog.info("log.info.clear_handlers_for_script", scriptName)
    }

    /**
     * 注册 Lua 事件处理器
     *
     * @param basePackage 事件类的基础包名
     * @param eventName 事件类的名称
     * @param handler Lua 事件处理函数
     */
    fun registerLuaEventHandler(basePackage: String, eventName: String, handler: LuaFunction) {
        try {
            // 尝试加载事件类
            val eventClassName = "$basePackage.$eventName"
            val eventClass = Class.forName(eventClassName) as Class<out Event>

            // 添加处理器到事件类的处理器列表中，并记录当前脚本名称
            if (!luaEventHandlers.containsKey(eventClass)) {
                luaEventHandlers[eventClass] = mutableListOf()
            }
            luaEventHandlers[eventClass]?.add(Pair(currentScriptName, handler))

            // 如果这是第一次为此事件类型注册处理器，则注册 Bukkit 监听器
            if (!registeredListenerTypes.contains(eventClass)) {
                registerBukkitListener(eventClass)
                registeredListenerTypes.add(eventClass)
            }

            PLog.info("log.info.register_event_handler", eventName, currentScriptName)
        } catch (e: ClassNotFoundException) {
            PLog.warning("log.warning.event_not_found", basePackage, eventName)
        } catch (e: Exception) {
            PLog.warning("log.warning.register_event_handler_failed", e.message ?: "Unknown error")
        }
    }

    /**
     * 为指定的事件类型注册 Bukkit 监听器
     *
     * @param eventClass 事件类
     */
    private fun registerBukkitListener(eventClass: Class<out Event>) {
        plugin.server.pluginManager.registerEvent(
            eventClass,
            this,
            EventPriority.NORMAL,
            { _, event ->
                if (eventClass.isInstance(event)) {
                    handleEvent(event)
                }
            },
            plugin
        )
        PLog.info("log.info.register_bukkit_listener", eventClass.simpleName)
    }

    /**
     * 处理事件，调用注册的所有 Lua 事件处理函数
     *
     * @param event Bukkit 事件对象
     */
    private fun handleEvent(event: Event) {
        val eventClass = event.javaClass
        val handlers = luaEventHandlers[eventClass] ?: return

        // 将 Java 事件对象转换为 Lua 值
        val luaEvent = LuaValueFactory.createLuaValue(event)

        // 调用所有注册的 Lua 处理函数
        for ((scriptName, handler) in handlers) {
            try {
                handler.call(luaEvent)
            } catch (e: LuaError) {
                PLog.warning("log.warning.handle_event_failed", scriptName, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * 清除所有 Lua 事件处理器，并注销所有 Bukkit 监听器
     */
    fun clearHandlers() {
        luaEventHandlers.clear()
        // 取消注册所有动态监听器
        HandlerList.unregisterAll(this)
        registeredListenerTypes.clear()
        PLog.info("log.info.clear_handlers")
    }
}