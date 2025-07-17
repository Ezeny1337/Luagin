package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import org.bukkit.inventory.ItemStack
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.gui.GuiContainer

object GuiAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()
    private val containerMap = mutableMapOf<String, GuiContainer>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 gui 表
        lua.newTable()

        // gui.create_container(gui_id: string): container - 创建一个容器包含不同玩家的 inventory
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) return@push 0
            val guiId = luaState.toString(1) ?: return@push 0
            val container = containerMap.getOrPut(guiId) { GuiContainer(guiId) }
            pushContainer(lua, container)
            return@push 1
        }
        lua.setField(-2, "create_container")

        lua.setGlobal("gui")

        if (!apiNames.contains("gui")) {
            apiNames.add("gui")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun pushContainer(lua: Lua, container: GuiContainer) {
        lua.newTable()

        // 绑定 GuiContainer 实例
        lua.pushJavaObject(container)
        lua.setField(-2, "__container")

        // create_inventory(player_name: string, title: string, size: number[, storable: boolean]) - 创建 inventory
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 4) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val title = luaState.toString(3) ?: "GUI"
            val size = luaState.toInteger(4).toInt()
            val storable = if (luaState.top >= 5) luaState.toBoolean(5) else false

            containerObj.getOrCreateInventory(playerName, title, size, storable)
            return@push 1
        }
        lua.setField(-2, "create_inventory")

        // open(player_name: string) - 打开 inventory
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.open(playerName)
            return@push 0
        }
        lua.setField(-2, "open")

        // close(player_name: string) - 关闭 inventory
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.close(playerName)
            return@push 0
        }
        lua.setField(-2, "close")

        // set_item(player_name: string, slot: number, item: item, on_click: function) - 设置物品
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 4) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val slot = luaState.toInteger(3).toInt() - 1

            luaState.getField(4, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack
            luaState.pop(1)

            if (item == null) return@push 0

            var onClick: ((String, String) -> Unit)? = null
            if (luaState.top >= 5 && luaState.isFunction(5)) {
                val callbackRef = luaState.ref()
                onClick = { player, clickType ->
                    lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                    lua.push(player)
                    lua.push(clickType)
                    lua.pCall(2, 0)
                }
            }

            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.setItem(slot, item, onClick)
            return@push 0
        }
        lua.setField(-2, "set_item")

        // remove_item(player_name: string, slot: number) - 移除物品
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val slot = luaState.toInteger(3).toInt() - 1
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.removeItem(slot)
            return@push 0
        }
        lua.setField(-2, "remove_item")

        // set_slot_interact(player_name: string, slot: number, can_put: boolean, can_take: boolean) - 设置槽位交互性
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 5) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val slot = luaState.toInteger(3).toInt() - 1
            val canPut = luaState.toBoolean(4)
            val canTake = luaState.toBoolean(5)
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.setSlotInteractive(slot, canPut, canTake)
            return@push 0
        }
        lua.setField(-2, "set_slot_interact")

        // on_open(player_name: string, callback: function) - 设置打开 inventory 回调
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3 || !luaState.isFunction(3)) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val callbackRef = luaState.ref()
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.onOpen { player ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.push(player)
                lua.pCall(1, 0)
            }
            return@push 0
        }
        lua.setField(-2, "on_open")

        // on_close(player_name: string, callback: function) - 设置关闭 inventory 回调
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3 || !luaState.isFunction(3)) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val callbackRef = luaState.ref()
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.onClose { player ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.push(player)
                lua.pCall(1, 0)
            }
            return@push 0
        }
        lua.setField(-2, "on_close")

        // animate(player_name: string, duration: number, interval: number, animation_fn: function) - 设置 inventory 动画
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? GuiContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 5 || !luaState.isNumber(3) || !luaState.isNumber(4) || !luaState.isFunction(
                    5
                )
            ) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val duration = luaState.toInteger(3).toInt()
            val interval = luaState.toInteger(4).toInt()
            val callbackRef = luaState.ref()
            val gui = containerObj.getInventory(playerName) ?: return@push 0

            gui.animate(duration, interval) { _, tick ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.pushJavaObject(containerObj)
                lua.push(playerName)
                lua.push(tick)
                lua.pCall(3, 0)
            }
            return@push 0
        }
        lua.setField(-2, "animate")
    }
}