package tech.ezeny.luaKit.lua.api

import org.bukkit.plugin.java.JavaPlugin
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luaKit.events.EventManager
import tech.ezeny.luaKit.utils.PLog

object EventsAPI : LuaAPIProvider {
    private lateinit var plugin: JavaPlugin
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: JavaPlugin) {
        this.plugin = plugin
    }

    override fun registerAPI(globals: Globals) {
        // 在 Lua 环境中创建全局 events table
        val eventsTable = LuaTable()
        globals.set("events", eventsTable)

        // 获取事件分类和对应的包名
        val eventCategories = EventManager.getEventCategories()

        // 为不同事件类型创建子表和函数
        eventCategories.forEach { (categoryName, basePackage) ->
            val categoryTable = LuaTable()

            // 注册事件处理器的方法
            categoryTable.set("set", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val eventName = args.checkjstring(2)
                    val handler = args.checkfunction(3)

                    EventManager.registerLuaEventHandler(basePackage, eventName, handler)
                    return NIL
                }
            })

            // 取消事件处理器的方法
            categoryTable.set("unset", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    if (args.narg() != 2 || !args.arg(2).isstring()) {
                        return NIL
                    }

                    val eventName = args.arg(2).tojstring()
                    EventManager.unsetLuaEventHandler(basePackage, eventName)
                    return NIL
                }
            })

            eventsTable.set(categoryName, categoryTable)
        }

        if (!apiNames.contains("events")) {
            apiNames.add("events")
        }

        PLog.info("log.info.events_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
}