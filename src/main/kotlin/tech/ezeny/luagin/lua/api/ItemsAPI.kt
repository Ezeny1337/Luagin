package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.inventory.ItemStack
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.items.ItemManager
import tech.ezeny.luagin.utils.PLog

object ItemsAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val itemManager: ItemManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 创建 items 表
        lua.newTable()

        // create 函数 - 创建基础物品
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val materialName = luaState.toString(1) ?: return@push 0

            val material = Material.valueOf(materialName.uppercase())
            val item = itemManager.createItem(material)
            pushItem(lua, item)
            return@push 1
        }
        lua.setField(-2, "create")

        // create_custom 函数 - 创建自定义物品
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val materialName = luaState.toString(1) ?: return@push 0
            val displayName = if (luaState.top > 1 && luaState.isString(2)) luaState.toString(2) else null
            val lore = if (luaState.top > 2 && luaState.isTable(3)) {
                val loreList = mutableListOf<String>()
                luaState.pushNil()
                while (luaState.next(3) != 0) {
                    if (luaState.isString(-1)) {
                        loreList.add(luaState.toString(-1) ?: "")
                    }
                    luaState.pop(1)
                }
                loreList
            } else null

            val material = Material.valueOf(materialName.uppercase())
            val item = itemManager.createCustomItem(
                material = material,
                displayName = displayName,
                lore = lore
            )
            pushItem(lua, item)
            return@push 1
        }
        lua.setField(-2, "create_custom")

        // create_skull 函数 - 创建头颅
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val owner = luaState.toString(1) ?: return@push 0
            val displayName = if (luaState.top > 1 && luaState.isString(2)) luaState.toString(2) else null
            val lore = if (luaState.top > 2 && luaState.isTable(3)) {
                val loreList = mutableListOf<String>()
                luaState.pushNil()
                while (luaState.next(3) != 0) {
                    if (luaState.isString(-1)) {
                        loreList.add(luaState.toString(-1) ?: "")
                    }
                    luaState.pop(1)
                }
                loreList
            } else null

            val item = itemManager.createSkull(owner, displayName, lore)
            pushItem(lua, item)
            return@push 1
        }
        lua.setField(-2, "create_skull")

        // give 函数 - 给玩家物品
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0

            // 检查第二个参数是否是物品表
            val item: ItemStack?
            if (luaState.isTable(2)) {
                // 从物品表中获取实际的ItemStack
                luaState.getField(2, "__item")
                item = luaState.toJavaObject(-1) as? ItemStack
                luaState.pop(1)
            } else {
                item = luaState.toJavaObject(2) as? ItemStack
            }

            if (item == null) {
                luaState.push(false)
                return@push 1
            }

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val success = itemManager.giveItem(player, item)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "give")

        // remove 函数 - 从玩家背包移除物品
        lua.push { luaState ->
            if (luaState.top < 3 || !luaState.isString(1) || !luaState.isString(2) || !luaState.isNumber(3)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val materialName = luaState.toString(2) ?: return@push 0
            val amount = luaState.toInteger(3).toInt()

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val material = Material.valueOf(materialName.uppercase())
            val success = itemManager.removeItem(player, material, amount)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "remove")

        // has 函数 - 检查玩家是否有指定物品
        lua.push { luaState ->
            if (luaState.top < 2 || !luaState.isString(1) || !luaState.isString(2)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val materialName = luaState.toString(2) ?: return@push 0
            val amount = if (luaState.top > 2 && luaState.isNumber(3)) luaState.toInteger(3).toInt() else 1

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val material = Material.valueOf(materialName.uppercase())
            val hasItem = itemManager.hasItem(player, material, amount)
            luaState.push(hasItem)
            return@push 1
        }
        lua.setField(-2, "has")

        // count 函数 - 获取玩家指定物品的数量
        lua.push { luaState ->
            if (luaState.top < 2 || !luaState.isString(1) || !luaState.isString(2)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val materialName = luaState.toString(2) ?: return@push 0

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(0)
                return@push 1
            }

            try {
                val material = Material.valueOf(materialName.uppercase())
                val count = itemManager.getItemCount(player, material)
                luaState.push(count.toLong())
                return@push 1
            } catch (e: IllegalArgumentException) {
                PLog.warning("无效的物品类型: $materialName")
                luaState.push(0)
                return@push 1
            }
        }
        lua.setField(-2, "count")

        // clear_inventory 函数 - 清空玩家背包
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val success = itemManager.clearInventory(player)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "clear_inventory")

        // set_inventory 函数 - 设置玩家背包内容
        lua.push { luaState ->
            if (luaState.top < 2 || !luaState.isString(1) || !luaState.isTable(2)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val items = mutableListOf<ItemStack>()
            luaState.pushNil()
            while (luaState.next(2) != 0) {
                val item: ItemStack?
                if (luaState.isTable(-1)) {
                    // 从物品表中获取实际的ItemStack
                    luaState.getField(-1, "__item")
                    item = luaState.toJavaObject(-1) as? ItemStack
                    luaState.pop(1)
                } else {
                    item = luaState.toJavaObject(-1) as? ItemStack
                }
                if (item != null) {
                    items.add(item)
                }
                luaState.pop(1)
            }

            val success = itemManager.setInventory(player, items)
            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "set_inventory")

        // get_inventory 函数 - 获取玩家背包内容
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                return@push 0
            }

            val items = itemManager.getInventory(player)
            luaState.newTable()
            items.forEachIndexed { index, item ->
                luaState.push((index + 1).toLong())
                pushItem(lua, item)
                luaState.setTable(-3)
            }
            return@push 1
        }
        lua.setField(-2, "get_inventory")

        lua.setGlobal("items")

        if (!apiNames.contains("items")) {
            apiNames.add("items")
        }
    }

    /**
     * 将物品推送到 Lua
     */
    private fun pushItem(lua: Lua, item: ItemStack) {
        // 创建物品表
        lua.newTable()

        lua.pushJavaObject(item)
        lua.setField(-2, "__item")

        // set_amount 方法 - 设置物品数量
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isNumber(2)) {
                return@push 0
            }

            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val amount = luaState.toInteger(2).toInt()

            val modifiedItem = itemManager.setAmount(item, amount)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_amount")

        // get_amount 方法 - 获取物品数量
        lua.push { luaState ->
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack
            luaState.pop(1)

            if (item == null) {
                return@push 0
            }

            val amount = itemManager.getAmount(item)
            luaState.push(amount.toLong())
            return@push 1
        }
        lua.setField(-2, "get_amount")

        // get_data 方法 - 获取物品持久化数据
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(2)) {
                return@push 0
            }

            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val key = luaState.toString(2) ?: return@push 0
            val namespace = if (luaState.top > 2 && luaState.isString(3)) luaState.toString(3) ?: "luagin" else "luagin"

            val value = itemManager.getPersistentData(item, key, namespace)
            if (value != null) {
                luaState.push(value)
                return@push 1
            } else {
                return@push 0
            }
        }
        lua.setField(-2, "get_data")

        // set_data 方法 - 设置物品持久化数据
        lua.push { luaState ->
            if (luaState.top < 2 || !luaState.isString(2) || !luaState.isString(3)) {
                return@push 0
            }

            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val key = luaState.toString(2) ?: return@push 0
            val value = luaState.toString(3) ?: return@push 0
            val namespace = if (luaState.top > 3 && luaState.isString(4)) luaState.toString(4) ?: "luagin" else "luagin"

            val modifiedItem = itemManager.setPersistentData(item, key, value, namespace)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_data")

        // has_data 方法 - 检查物品是否有持久化数据
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(2)) {
                return@push 0
            }

            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val key = luaState.toString(2) ?: return@push 0
            val namespace = if (luaState.top > 2 && luaState.isString(3)) luaState.toString(3) ?: "luagin" else "luagin"

            val hasData = itemManager.hasPersistentData(item, key, namespace)
            luaState.push(hasData)
            return@push 1
        }
        lua.setField(-2, "has_data")

        // get_info 方法 - 获取物品信息
        lua.push { luaState ->
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack
            luaState.pop(1)

            if (item == null) {
                return@push 0
            }

            val info = itemManager.getItemInfo(item)

            luaState.newTable()
            info.forEach { (key, value) ->
                luaState.push(key)
                when (value) {
                    is String -> luaState.push(value)
                    is Int -> luaState.push(value.toLong())
                    is Boolean -> luaState.push(value)
                    is List<*> -> {
                        luaState.newTable()
                        value.forEachIndexed { index, element ->
                            luaState.push((index + 1).toLong())
                            when (element) {
                                is String -> luaState.push(element)
                                is Int -> luaState.push(element.toLong())
                                else -> luaState.push(element.toString())
                            }
                            luaState.setTable(-3)
                        }
                    }

                    is Map<*, *> -> {
                        luaState.newTable()
                        value.forEach { (mapKey, mapValue) ->
                            luaState.push(mapKey.toString())
                            when (mapValue) {
                                is String -> luaState.push(mapValue)
                                is Int -> luaState.push(mapValue.toLong())
                                else -> luaState.push(mapValue.toString())
                            }
                            luaState.setTable(-3)
                        }
                    }

                    else -> luaState.push(value.toString())
                }
                luaState.setTable(-3)
            }
            return@push 1
        }
        lua.setField(-2, "get_info")

        lua.getGlobal("items")
        lua.getField(-1, "__metatable")
        lua.setMetatable(-3)
        lua.pop(1)
    }

    override fun getAPINames(): List<String> = apiNames
}