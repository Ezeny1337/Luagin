package tech.ezeny.luaKit.events

import org.bukkit.event.Event
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaFunction
import tech.ezeny.luaKit.LuaKit
import tech.ezeny.luaKit.utils.PLog
import tech.ezeny.luaKit.lua.LuaValueFactory

class EventManager(private val plugin: LuaKit) : Listener {
    // 存储 Lua 处理函数，按 Event 类分组
    private val luaEventHandlers = mutableMapOf<Class<out Event>, MutableList<Pair<String, LuaFunction>>>()

    // 跟踪已经为哪些 Event 类注册了 Bukkit 监听器
    private val registeredListenerTypes = mutableSetOf<Class<out Event>>()

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

    // 当前正在加载的脚本文件名
    private var currentScriptName: String = ""

    // 获取事件分类和包名映射
    fun getEventCategories(): Map<String, String> = eventCategories

    // 设置当前正在加载的脚本名称
    fun setCurrentScript(scriptName: String) {
        currentScriptName = scriptName
    }

    // 清除特定脚本的事件处理器
    fun clearHandlersForScript(scriptName: String) {
        // 遍历所有事件类型
        val eventClassesToRemove = mutableListOf<Class<out Event>>()

        luaEventHandlers.forEach { (eventClass, handlers) ->
            // 移除属于指定脚本的处理器
            val handlersToRemove = handlers.filter { it.first == scriptName }
            handlers.removeAll(handlersToRemove)

            if (handlers.isEmpty()) {
                eventClassesToRemove.add(eventClass)
            }
        }

        // 移除没有处理器的事件类型
        eventClassesToRemove.forEach { luaEventHandlers.remove(it) }

        PLog.info("log.info.clear_handlers_for_script", scriptName)
    }

    // 注册事件处理器方法
    fun registerLuaEventHandler(basePackage: String, eventName: String, handler: LuaFunction) {
        try {
            // 尝试加载事件类
            val eventClassName = "$basePackage.$eventName"
            val eventClass = Class.forName(eventClassName) as Class<out Event>

            // 将处理器添加到映射中，并记录脚本名称
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
            PLog.warning("log.warning.register_event_handler_error", e.message ?: "Unknown error")
        }
    }

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
                PLog.warning("log.warning.handle_event_error", scriptName, e.message ?: "Unknown error")
            }
        }
    }

    fun clearHandlers() {
        luaEventHandlers.clear()
        // 取消注册所有动态监听器
        HandlerList.unregisterAll(this)
        registeredListenerTypes.clear()
        PLog.info("log.info.clear_handlers")
    }
}