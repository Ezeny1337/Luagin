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

        // create(material: string): item - 创建基础物品
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val materialName = luaState.toString(1) ?: return@push 0
            val material = enumValueOf<Material>(materialName)
            val item = itemManager.createItem(material)
            pushItem(lua, item)
            return@push 1
        }
        lua.setField(-2, "create")

        // create_custom(material: string[, display_name: string, lore: string]): item - 创建自定义物品
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(1)) {
                return@push 0
            }

            val materialName = luaState.toString(1) ?: return@push 0
            val material = enumValueOf<Material>(materialName)
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

            val item = itemManager.createCustomItem(
                material = material,
                displayName = displayName,
                lore = lore
            )
            pushItem(lua, item)
            return@push 1
        }
        lua.setField(-2, "create_custom")

        // create_skull(owner: string[, display_name: string, lore: string]): item - 创建头颅
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

        // give(player_name: string, item: item[, amount: number]): boolean - 给予玩家物品
        // give(player_name: string, material: string[, amount: number]): boolean
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val amount = if (luaState.top > 2 && luaState.isNumber(3)) luaState.toInteger(3).toInt() else 1

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val success: Boolean
            if (luaState.isString(2)) {
                // 第二个参数是字符串
                val materialName = luaState.toString(2) ?: return@push 0
                val material = enumValueOf<Material>(materialName.uppercase())
                val item = itemManager.createItem(material)
                item.amount = amount
                success = itemManager.giveItem(player, item)
            } else {
                // 第二个参数是物品对象
                val item: ItemStack?
                if (luaState.isTable(2)) {
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

                // 设置物品数量
                item.amount = amount
                success = itemManager.giveItem(player, item)
            }

            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "give")

        // remove(player_name: string, item: item[, amount: number]): boolean - 移除玩家物品
        // remove(player_name: string, material: string[, amount: number]): boolean
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val amount = if (luaState.top > 2 && luaState.isNumber(3)) luaState.toInteger(3).toInt() else 1

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val success: Boolean
            if (luaState.isString(2)) {
                // 第二个参数是字符串
                val materialName = luaState.toString(2) ?: return@push 0
                success = itemManager.removeItem(player, itemManager.materialToItemStack(materialName), amount)
            } else {
                // 第二个参数是物品对象
                val item: ItemStack?
                if (luaState.isTable(2)) {
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
                success = itemManager.removeItem(player, item, amount)
            }

            luaState.push(success)
            return@push 1
        }
        lua.setField(-2, "remove")

        // has(player_name: string, item: item[, amount: number]): boolean - 检查玩家是否拥有物品
        // has(player_name: string, material: string[, amount: number]): boolean
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0
            val amount = if (luaState.top > 2 && luaState.isNumber(3)) luaState.toInteger(3).toInt() else 1

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(false)
                return@push 1
            }

            val hasItem: Boolean
            if (luaState.isString(2)) {
                // 第二个参数是字符串
                val materialName = luaState.toString(2) ?: return@push 0

                hasItem = itemManager.hasItem(player, itemManager.materialToItemStack(materialName), amount)
            } else {
                // 第二个参数是物品对象
                val item: ItemStack?
                if (luaState.isTable(2)) {
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

                hasItem = itemManager.hasItem(player, item, amount)
            }

            luaState.push(hasItem)
            return@push 1
        }
        lua.setField(-2, "has")

        // count(player_name: string, item: item): number - 获取玩家拥有的物品数量
        // count(player_name: string, material: string): number
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: return@push 0

            val player = Bukkit.getPlayer(playerName)
            if (player == null) {
                luaState.push(0)
                return@push 1
            }

            val count: Int
            if (luaState.isString(2)) {
                // 第二个参数是字符串，按材质处理
                val materialName = luaState.toString(2) ?: return@push 0

                count = itemManager.getItemCount(player, itemManager.materialToItemStack(materialName))
            } else {
                // 第二个参数是物品对象，按精确匹配处理
                val item: ItemStack?
                if (luaState.isTable(2)) {
                    luaState.getField(2, "__item")
                    item = luaState.toJavaObject(-1) as? ItemStack
                    luaState.pop(1)
                } else {
                    item = luaState.toJavaObject(2) as? ItemStack
                }

                if (item == null) {
                    luaState.push(0)
                    return@push 1
                }
                count = itemManager.getItemCount(player, item)
            }

            luaState.push(count.toLong())
            return@push 1
        }
        lua.setField(-2, "count")

        // clear_inventory(player_name: string): boolean - 清空玩家背包
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

        // set_inventory(player_name: string, items: table): boolean - 设置玩家背包内容
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

        // get_inventory(player_name: string): boolean - 获取玩家背包内容
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

    override fun getAPINames(): List<String> = apiNames

    /**
     * 将物品推送到 Lua
     */
    private fun pushItem(lua: Lua, item: ItemStack) {
        lua.newTable()

        // 绑定 ItemStack 实例
        lua.pushJavaObject(item)
        lua.setField(-2, "__item")

        // set_amount(amount: number): item - 设置物品数量
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

        // get_amount(): number - 获取物品数量
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

        // set_data(key: string, value: string[, namespace: string]): item - 设置物品持久化数据
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

        // get_data(key: string[, namespace: string]): string - 获取物品持久化数据
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

        // has_data(key: string[, namespace: string]): boolean - 检查物品是否有持久化数据
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

        // set_enchant(enchant: string, level: number): item - 设置附魔
        lua.push { luaState ->
            if (luaState.top < 2 || !luaState.isString(2) || !luaState.isNumber(3)) {
                return@push 0
            }
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val enchantName = luaState.toString(2) ?: return@push 0
            val level = luaState.toInteger(3).toInt()
            val modifiedItem = itemManager.setEnchant(item, enchantName, level)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_enchant")

        // set_flag(flag: string): item - 设置物品标志
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isString(2)) {
                return@push 0
            }
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val flagName = luaState.toString(2) ?: return@push 0
            val modifiedItem = itemManager.addItemFlag(item, flagName)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_flag")

        // set_unbreakable(unbreakable: boolean): item - 设置不可破坏
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isBoolean(2)) {
                return@push 0
            }
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val unbreakable = luaState.toBoolean(2)
            val modifiedItem = itemManager.setUnbreakable(item, unbreakable)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_unbreakable")

        // set_custom_model_data(data: number): item - 设置自定义模型数据
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isNumber(2)) {
                return@push 0
            }
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val data = luaState.toInteger(2).toInt()
            val modifiedItem = itemManager.setCustomModelData(item, data)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_custom_model_data")

        // set_durability(durability: number): item - 设置物品耐久度
        lua.push { luaState ->
            if (luaState.top < 1 || !luaState.isNumber(2)) {
                return@push 0
            }
            luaState.getField(1, "__item")
            val item = luaState.toJavaObject(-1) as? ItemStack ?: return@push 0
            luaState.pop(1)

            val durability = luaState.toInteger(2).toInt()
            val modifiedItem = itemManager.setDurability(item, durability)
            pushItem(lua, modifiedItem)
            return@push 1
        }
        lua.setField(-2, "set_durability")

        // get_info(): iteminfo - 获取物品信息
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
}