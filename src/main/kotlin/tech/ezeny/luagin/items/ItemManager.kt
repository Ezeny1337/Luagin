package tech.ezeny.luagin.items

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.SkullMeta
import org.bukkit.persistence.PersistentDataType

class ItemManager {

    /**
     * 创建基础物品
     * @param material 物品材质
     * @return 创建的物品堆栈
     */
    fun createItem(material: Material): ItemStack {
        return ItemStack(material, 1)
    }

    /**
     * 创建自定义物品
     *
     * @param material 物品材质
     * @param displayName 显示名称
     * @param lore 物品描述列表
     * @param enchantments 附魔效果映射，键为附魔类型，值为等级
     * @param itemFlags 物品标志集合，用于隐藏某些属性
     * @param unbreakable 是否不可破坏
     * @param customModelData 自定义模型数据，用于资源包
     * @return 创建的自定义物品
     */
    fun createCustomItem(
        material: Material,
        displayName: String? = null,
        lore: List<String>? = null,
        enchantments: Map<Enchantment, Int>? = null,
        itemFlags: Set<ItemFlag>? = null,
        unbreakable: Boolean = false,
        customModelData: Int? = null
    ): ItemStack {
        val item = ItemStack(material, 1)
        val meta = item.itemMeta ?: return item

        // 设置显示名称
        if (displayName != null) {
            meta.setDisplayName(displayName)
        }

        // 设置Lore
        if (lore != null) {
            meta.lore = lore
        }

        // 设置附魔
        enchantments?.forEach { (enchantment, level) ->
            meta.addEnchant(enchantment, level, true)
        }

        // 设置物品标志
        if (itemFlags != null) {
            meta.addItemFlags(*itemFlags.toTypedArray())
        }

        // 设置不可破坏
        meta.isUnbreakable = unbreakable

        // 设置自定义模型数据 - 使用新的API
        if (customModelData != null) {
            val component = meta.getCustomModelDataComponent()
            component.setFloats(listOf(customModelData.toFloat()))
            meta.setCustomModelDataComponent(component)
        }

        item.itemMeta = meta
        return item
    }

    /**
     * 创建头颅物品
     *
     * @param owner 头颅所有者的名称
     * @param displayName 显示名称
     * @param lore 物品描述列表
     * @return 创建的头颅物品
     */
    fun createSkull(
        owner: String,
        displayName: String? = null,
        lore: List<String>? = null
    ): ItemStack {
        val item = ItemStack(Material.PLAYER_HEAD, 1)
        val meta = item.itemMeta as? SkullMeta ?: return item

        // 设置头颅所有者
        meta.owningPlayer = Bukkit.getOfflinePlayer(owner)

        // 设置显示名称
        if (displayName != null) {
            meta.setDisplayName(displayName)
        }

        // 设置Lore
        if (lore != null) {
            meta.lore = lore
        }

        item.itemMeta = meta
        return item
    }

    /**
     * 设置物品数量
     *
     */
    fun setAmount(item: ItemStack, amount: Int): ItemStack {
        item.amount = amount.coerceIn(1, item.maxStackSize)
        return item
    }

    /**
     * 获取物品数量
     */
    fun getAmount(item: ItemStack): Int {
        return item.amount
    }

    /**
     * 设置物品的持久化数据
     *
     * @param item 目标物品
     * @param key 数据键名
     * @param value 数据值
     * @param namespace 命名空间，默认为 luagin
     * @return 设置数据后的物品
     */
    fun setPersistentData(
        item: ItemStack,
        key: String,
        value: String,
        namespace: String = "luagin"
    ): ItemStack {
        val meta = item.itemMeta ?: return item
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey(namespace, key)
        container.set(namespacedKey, PersistentDataType.STRING, value)
        item.itemMeta = meta
        return item
    }

    /**
     * 获取物品的持久化数据
     */
    fun getPersistentData(
        item: ItemStack,
        key: String,
        namespace: String = "luagin"
    ): String? {
        val meta = item.itemMeta ?: return null
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey(namespace, key)
        return container.get(namespacedKey, PersistentDataType.STRING)
    }

    /**
     * 检查物品是否有持久化数据
     */
    fun hasPersistentData(
        item: ItemStack,
        key: String,
        namespace: String = "luagin"
    ): Boolean {
        val meta = item.itemMeta ?: return false
        val container = meta.persistentDataContainer
        val namespacedKey = NamespacedKey(namespace, key)
        return container.has(namespacedKey, PersistentDataType.STRING)
    }

    /**
     * 给玩家物品
     */
    fun giveItem(player: Player, item: ItemStack): Boolean {
        val result = player.inventory.addItem(item)
        return result.isEmpty()
    }

    /**
     * 从玩家背包移除物品
     */
    fun removeItem(player: Player, material: Material, amount: Int): Boolean {
        val result = player.inventory.removeItem(ItemStack(material, amount))
        return result.isEmpty()
    }

    /**
     * 检查玩家是否有指定物品
     */
    fun hasItem(player: Player, material: Material, amount: Int = 1): Boolean {
        var count = 0
        for (item in player.inventory.contents) {
            if (item?.type == material) {
                count += item.amount
                if (count >= amount) return true
            }
        }
        return false
    }

    /**
     * 获取玩家指定物品的数量
     */
    fun getItemCount(player: Player, material: Material): Int {
        var count = 0
        for (item in player.inventory.contents) {
            if (item?.type == material) {
                count += item.amount
            }
        }
        return count
    }

    /**
     * 清空玩家背包
     */
    fun clearInventory(player: Player): Boolean {
        return try {
            player.inventory.clear()
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 设置玩家背包内容
     *
     * @param player 目标玩家
     * @param items 要设置的物品列表
     * @return 是否成功设置背包
     */
    fun setInventory(player: Player, items: List<ItemStack>): Boolean {
        return try {
            player.inventory.clear()
            items.forEachIndexed { index, item ->
                if (index < 36) { // 背包最多36个槽位
                    player.inventory.setItem(index, item)
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * 获取玩家背包内容
     */
    fun getInventory(player: Player): List<ItemStack> {
        return player.inventory.contents.filterNotNull()
    }


    /**
     * 获取物品信息
     */
    fun getItemInfo(item: ItemStack): Map<String, Any> {
        val info = mutableMapOf<String, Any>()

        info["type"] = item.type.name
        info["amount"] = item.amount
        info["max_stack_size"] = item.maxStackSize

        val meta = item.itemMeta
        if (meta != null) {
            if (meta.hasDisplayName()) {
                info["display_name"] = meta.displayName
            }
            if (meta.hasLore()) {
                info["lore"] = meta.lore ?: emptyList<String>()
            }
            if (meta.hasEnchants()) {
                info["enchantments"] = meta.enchants.mapKeys { it.key.keyOrThrow }
            }
            info["unbreakable"] = meta.isUnbreakable
            if (meta.hasCustomModelData()) {
                info["custom_model_data"] = meta.customModelData
            }
        }

        return info
    }
} 