package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import org.bukkit.inventory.ItemStack
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.gui.inventory.InventoryContainer
import tech.ezeny.luagin.gui.scoreboard.ScoreboardContainer

object GuiAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val apiNames = mutableListOf<String>()
    private val inventoryContainerMap = mutableMapOf<String, InventoryContainer>()
    private val scoreboardContainerMap = mutableMapOf<String, ScoreboardContainer>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 gui 表
        lua.newTable()

        // gui.create_inventory_ctr(gui_id: string): ctr - 创建一个 inventory 容器
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) return@push 0
            val guiId = luaState.toString(1) ?: return@push 0
            val container = inventoryContainerMap.getOrPut(guiId) { InventoryContainer(guiId) }
            pushInventoryCtr(lua, container)
            return@push 1
        }
        lua.setField(-2, "create_inventory_ctr")

        // gui.create_scoreboard_ctr(gui_id: string): ctr - 创建一个 scoreboard 容器
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) return@push 0
            val guiId = luaState.toString(1) ?: return@push 0
            val container = scoreboardContainerMap.getOrPut(guiId) { ScoreboardContainer(guiId) }
            pushScoreboardCtr(lua, container)
            return@push 1
        }
        lua.setField(-2, "create_scoreboard_ctr")

        lua.setGlobal("gui")

        if (!apiNames.contains("gui")) {
            apiNames.add("gui")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun pushInventoryCtr(lua: Lua, container: InventoryContainer) {
        lua.newTable()

        // 绑定 InventoryContainer 实例
        lua.pushJavaObject(container)
        lua.setField(-2, "__container")

        // create_inventory(player_name: string, title: string, size: number[, storable: boolean]) - 创建 inventory
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
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

        // remove_inventory(player_name: string) - 移除玩家的 inventory
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? InventoryContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            containerObj.removeInventory(playerName)
            return@push 0
        }
        lua.setField(-2, "remove_inventory")
    }

    private fun pushScoreboardCtr(lua: Lua, container: ScoreboardContainer) {
        lua.newTable()

        // 绑定 ScoreboardContainer 实例
        lua.pushJavaObject(container)
        lua.setField(-2, "__container")

        // create_scoreboard(player_name: string, title: string) - 创建 scoreboard
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val title = luaState.toString(3) ?: "Scoreboard"

            containerObj.getOrCreateScoreboard(playerName, title)
            return@push 1
        }
        lua.setField(-2, "create_scoreboard")

        // show(player_name: string) - 显示 scoreboard
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.show(playerName)
            return@push 0
        }
        lua.setField(-2, "show")

        // hide(player_name: string) - 隐藏 scoreboard
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.hide(playerName)
            return@push 0
        }
        lua.setField(-2, "hide")

        // set_title(player_name: string, title: string) - 设置标题
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val title = luaState.toString(3) ?: return@push 0
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.setTitle(title)
            return@push 0
        }
        lua.setField(-2, "set_title")

        // set_line(player_name: string, line: number, text: string) - 设置行内容
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 4) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val line = luaState.toInteger(3).toInt()
            val text = luaState.toString(4) ?: ""
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.setLine(line, text)
            return@push 0
        }
        lua.setField(-2, "set_line")

        // set_lines(player_name: string, lines: table) - 设置多行内容
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3 || !luaState.isTable(3)) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            val lines = mutableListOf<String>()
            luaState.pushValue(3)
            luaState.pushNil()
            while (luaState.next(-2) != 0) {
                if (luaState.isString(-1)) {
                    lines.add(luaState.toString(-1) ?: "")
                }
                luaState.pop(1)
            }
            luaState.pop(1)

            scoreboard.setLines(lines)
            return@push 0
        }
        lua.setField(-2, "set_lines")

        // clear_line(player_name: string, line: number) - 清除行
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val line = luaState.toInteger(3).toInt()
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.clearLine(line)
            return@push 0
        }
        lua.setField(-2, "clear_line")

        // clear_all_lines(player_name: string) - 清除所有行
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.clearAllLines()
            return@push 0
        }
        lua.setField(-2, "clear_all_lines")

        // on_show(player_name: string, callback: function) - 设置显示回调
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3 || !luaState.isFunction(3)) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val callbackRef = luaState.ref()
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.onShow { player ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.push(player)
                lua.pCall(1, 0)
            }
            return@push 0
        }
        lua.setField(-2, "on_show")

        // on_hide(player_name: string, callback: function) - 设置隐藏回调
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 3 || !luaState.isFunction(3)) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val callbackRef = luaState.ref()
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.onHide { player ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.push(player)
                lua.pCall(1, 0)
            }
            return@push 0
        }
        lua.setField(-2, "on_hide")

        // animate(player_name: string, duration: number, interval: number, animation_fn: function) - 设置 scoreboard 动画
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 5 || !luaState.isNumber(3) || !luaState.isNumber(4) || !luaState.isFunction(
                    5
                )
            ) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            val duration = luaState.toInteger(3).toInt()
            val interval = luaState.toInteger(4).toInt()
            val callbackRef = luaState.ref()
            val scoreboard = containerObj.getScoreboard(playerName) ?: return@push 0

            scoreboard.animate(duration, interval) { _, tick ->
                lua.rawGetI(LUA_REGISTRYINDEX, callbackRef)
                lua.pushJavaObject(containerObj)
                lua.push(playerName)
                lua.push(tick)
                lua.pCall(3, 0)
            }
            return@push 0
        }
        lua.setField(-2, "animate")

        // remove_scoreboard(player_name: string) - 移除玩家的记分板
        lua.push { luaState ->
            luaState.getField(1, "__container")
            val containerObj = luaState.toJavaObject(-1) as? ScoreboardContainer
            luaState.pop(1)

            if (containerObj == null || luaState.top < 2) return@push 0

            val playerName = luaState.toString(2) ?: return@push 0
            containerObj.removeScoreboard(playerName)
            return@push 0
        }
        lua.setField(-2, "remove_scoreboard")
    }
}