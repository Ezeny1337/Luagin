package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.events.EventManager
import tech.ezeny.luagin.events.LuaJITEventHandler

object EventsAPI : LuaAPIProvider, KoinComponent {
    private val eventManager: EventManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(lua: Lua) {
        // 创建 events 表
        lua.newTable()

        // 获取事件分类和对应的包名
        val eventCategories = eventManager.getEventCategories()

        // 为不同事件类型创建子表和函数
        eventCategories.forEach { (categoryName, basePackage) ->
            lua.newTable()

            // set(event_name:string, callback: function)
            lua.push { luaState ->
                if (luaState.top < 2) {
                    return@push 0
                }

                val eventName = luaState.toString(2) ?: ""
                val handlerIndex = 3

                if (!luaState.isFunction(handlerIndex)) {
                    return@push 0
                }

                // 创建对 Lua 函数的引用
                luaState.pushValue(handlerIndex)
                val functionRef = luaState.ref(LUA_REGISTRYINDEX)

                // 创建一个包装函数来调用 Lua 处理器
                val luaHandler = LuaJITEventHandler(lua, functionRef)
                eventManager.registerLuaEventHandler(basePackage, eventName, luaHandler)

                return@push 0
            }
            lua.setField(-2, "set")

            lua.setField(-2, categoryName)
        }

        lua.setGlobal("events")

        if (!apiNames.contains("events")) {
            apiNames.add("events")
        }
    }

    override fun getAPINames(): List<String> = apiNames
}