package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.utils.PLog

object EventsAPI : LuaAPIProvider, KoinComponent {
    private val eventManager: EventManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(globals: Globals) {
        // 创建 events 表
        val eventsTable = LuaTable()
        globals.set("events", eventsTable)

        // 获取事件分类和对应的包名
        val eventCategories = eventManager.getEventCategories()

        // 为不同事件类型创建子表和函数
        eventCategories.forEach { (categoryName, basePackage) ->
            val categoryTable = LuaTable()

            // 注册事件处理器的方法
            categoryTable.set("set", object : VarArgFunction() {
                override fun invoke(args: Varargs): Varargs {
                    val eventName = args.checkjstring(2)
                    val handler = args.checkfunction(3)

                    eventManager.registerLuaEventHandler(basePackage, eventName, handler)
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