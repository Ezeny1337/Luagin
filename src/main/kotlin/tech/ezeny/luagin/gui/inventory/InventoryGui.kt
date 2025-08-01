package tech.ezeny.luagin.gui.inventory

import org.bukkit.Bukkit
import org.bukkit.event.inventory.InventoryAction
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.ColorUtils
import kotlin.collections.HashMap
import kotlin.collections.iterator
class InventoryGui(
    title: String,
    val size: Int,
    val guiId: String,
    val storable: Boolean
) {
    private var colorizedTitle = ColorUtils.formatString(title)

    val inventory = Bukkit.createInventory(null, size, colorizedTitle)
    private val itemCallbacks = HashMap<Int, (player: String, clickType: String) -> Unit>()
    private val openCallbacks = mutableListOf<(player: String) -> Unit>()
    private val closeCallbacks = mutableListOf<(player: String) -> Unit>()

    // 跟踪脚本设置的物品槽位
    private val scriptSetSlots = mutableSetOf<Int>()

    // 初始化所有槽位为 Pair(false, false)
    private val slotInteractive = HashMap<Int, Pair<Boolean, Boolean>>(size).apply {
        for (i in 0 until size) put(i, Pair(false, false))
    }
    private var animationTask: BukkitRunnable? = null

    fun open(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return
        // 如果是可存储的GUI，加载用户存储的物品
        if (storable) {
            loadStoredItems(playerName)
        }
        player.openInventory(inventory)
        openCallbacks.forEach { it(playerName) }
        InventoryGuiListener.registerGui(this)
    }

    fun close(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return
        if (player.openInventory.topInventory == inventory) {
            player.closeInventory()
        }
    }

    fun setItem(slot: Int, item: ItemStack, onClick: ((String, String) -> Unit)?) {
        inventory.setItem(slot, item)
        // 标记为脚本设置的槽位
        scriptSetSlots.add(slot)
        if (onClick != null) {
            itemCallbacks[slot] = onClick
        } else {
            itemCallbacks.remove(slot)
        }
    }

    fun removeItem(slot: Int) {
        inventory.clear(slot)
        itemCallbacks.remove(slot)
        // 移除脚本设置标记
        scriptSetSlots.remove(slot)
    }

    fun setSlotInteractive(slot: Int, canPut: Boolean, canTake: Boolean) {
        slotInteractive[slot] = Pair(canPut, canTake)
    }

    fun onOpen(callback: (String) -> Unit) {
        openCallbacks.add(callback)
    }

    fun onClose(callback: (String) -> Unit) {
        closeCallbacks.add(callback)
    }

    fun animate(duration: Int, interval: Int, animation: (InventoryGui, Int) -> Unit) {
        animationTask?.cancel()
        val plugin = Bukkit.getPluginManager().getPlugin("Luagin") as? Luagin ?: return
        animationTask = object : BukkitRunnable() {
            var tick = 0
            override fun run() {
                if (tick >= duration) {
                    cancel()
                    return
                }
                animation(this@InventoryGui, tick)
                tick += interval
            }
        }
        animationTask?.runTaskTimer(plugin, 0L, interval.toLong())
    }

    fun handleClick(event: InventoryClickEvent) {
        val slot = event.rawSlot
        val player = event.whoClicked.name
        val clickType = event.click.name

        // 定义所有取出相关的 InventoryAction
        val takeActions = setOf(
            InventoryAction.PICKUP_ALL,
            InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_ONE,
            InventoryAction.PICKUP_SOME,
            InventoryAction.COLLECT_TO_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.HOTBAR_SWAP,
            InventoryAction.DROP_ALL_SLOT,
            InventoryAction.DROP_ONE_SLOT,
            InventoryAction.SWAP_WITH_CURSOR
        )
        // 定义所有放入相关的 InventoryAction
        val putActions = setOf(
            InventoryAction.PLACE_ALL,
            InventoryAction.PLACE_ONE,
            InventoryAction.PLACE_SOME,
            InventoryAction.SWAP_WITH_CURSOR,
            InventoryAction.MOVE_TO_OTHER_INVENTORY
        )

        // shift+点击背包物品时，只有目标 slot 在允许放入的槽位列表时才允许，否则取消事件
        // 但是由于 Bukkit 机制，shift+点击只能放入第一个空槽位，无法重定向到其它槽位
        // 如果非要实现，则会需要更多屎山代码来维护
        if (event.action == InventoryAction.MOVE_TO_OTHER_INVENTORY && event.clickedInventory?.type != inventory.type) {
            val canPutSlots = slotInteractive.filter { it.value.first }.keys
            val movingItem = event.currentItem ?: return
            var canPut = false
            for (putSlot in canPutSlots) {
                val targetItem = inventory.getItem(putSlot)
                if (targetItem == null || (targetItem.isSimilar(movingItem) && targetItem.amount < targetItem.maxStackSize)) {
                    canPut = true
                    break
                }
            }
            if (!canPut) {
                event.isCancelled = true
                return
            }
        }

        // 只处理 GUI 区域
        if (slot in 0 until inventory.size) {
            val interactive = slotInteractive[slot] ?: Pair(false, false)
            // 禁止未授权的取出
            if (!interactive.second && event.action in takeActions) {
                event.isCancelled = true
            }
            // 禁止未授权的放入
            if (!interactive.first && event.action in putActions) {
                event.isCancelled = true
            }
            // 只在 GUI 区域回调点击事件
            if (event.clickedInventory == inventory) {
                itemCallbacks[slot]?.invoke(player, clickType)
            }
        }
    }

    fun handleOpen(event: InventoryOpenEvent) {
        if (event.inventory != inventory) return
        val player = event.player.name
        openCallbacks.forEach { it(player) }
    }

    fun handleClose(event: InventoryCloseEvent) {
        if (event.inventory != inventory) return
        val player = event.player.name

        // 处理物品返还逻辑
        if (!storable) {
            returnItemsToPlayer(player)
        } else {
            // 保存可存储的物品
            saveStoredItems(player)
        }

        closeCallbacks.forEach { it(player) }
    }

    fun returnItemsToPlayer(playerName: String) {
        val player = Bukkit.getPlayer(playerName) ?: return
        val playerInventory = player.inventory

        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot)
            if (item != null && !scriptSetSlots.contains(slot)) {
                // 只返还非脚本设置的物品
                // 尝试将物品添加到玩家背包
                val remaining = playerInventory.addItem(item)
                // 如果背包满了，掉落物品
                for (remainingItem in remaining.values) {
                    player.world.dropItemNaturally(player.location, remainingItem)
                }
                // 清空GUI槽位
                inventory.clear(slot)
            }
        }
    }

    fun saveStoredItems(playerName: String) {
        val items = mutableMapOf<Int, ItemStack>()
        for (slot in 0 until inventory.size) {
            val item = inventory.getItem(slot)
            // 只保存非脚本设置的物品
            if (item != null && !scriptSetSlots.contains(slot)) {
                items[slot] = item
            }
        }
        InventoryStorageManager.saveItems(guiId, playerName, items)
    }

    fun loadStoredItems(playerName: String) {
        val items = InventoryStorageManager.loadItems(guiId, playerName)
        for ((slot, item) in items) {
            // 只在非脚本设置的槽位加载用户物品
            if (slot in 0 until inventory.size && !scriptSetSlots.contains(slot)) {
                inventory.setItem(slot, item)
            }
        }
    }

    fun getViewers(): List<String> {
        return Bukkit.getOnlinePlayers()
            .filter { it.openInventory.topInventory == inventory }
            .map { it.name }
    }
}