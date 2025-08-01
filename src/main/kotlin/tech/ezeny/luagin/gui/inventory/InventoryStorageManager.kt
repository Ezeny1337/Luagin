package tech.ezeny.luagin.gui.inventory

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import org.bukkit.inventory.ItemStack
import org.bukkit.util.io.BukkitObjectInputStream
import org.bukkit.util.io.BukkitObjectOutputStream
import java.util.Base64
import tech.ezeny.luagin.utils.PLog
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import kotlin.collections.iterator

object InventoryStorageManager {
    private val gson: Gson = GsonBuilder().create()
    private val dataFolder = File("plugins/Luagin/data")
    private val storageFile = File(dataFolder, "gui_storage.json")
    private val storageData = mutableMapOf<String, Map<Int, String>>() // 存储Base64的物品数据

    init {
        if (!dataFolder.exists()) {
            dataFolder.mkdirs()
        }
        loadStorageData()
    }

    fun saveItems(guiId: String, playerName: String, items: Map<Int, ItemStack>) {
        val storageKey = "${guiId}:${playerName}"
        val encodedItems = items.mapValues { (_, item) -> itemToBase64(item) ?: "" }
        storageData[storageKey] = encodedItems
        saveStorageData()
    }

    fun loadItems(guiId: String, playerName: String): Map<Int, ItemStack> {
        val storageKey = "${guiId}:${playerName}"
        val encodedItems = storageData[storageKey] ?: return emptyMap()
        return encodedItems.mapNotNull { (slot, encodedItem) ->
            base64ToItem(encodedItem)?.let { item -> slot to item }
        }.toMap()
    }

    private fun loadStorageData() {
        if (!storageFile.exists()) {
            return
        }

        try {
            val type = object : TypeToken<Map<String, Map<String, String>>>() {}.type
            val jsonData = FileReader(storageFile).use { it.readText() }
            val loadedData = gson.fromJson<Map<String, Map<String, String>>>(jsonData, type)

            // 转换字符串键为整数键
            storageData.clear()
            for ((guiId, items) in loadedData) {
                val convertedItems = mutableMapOf<Int, String>()
                for ((slotStr, encodedItem) in items) {
                    try {
                        val slot = slotStr.toInt()
                        convertedItems[slot] = encodedItem
                    } catch (e: NumberFormatException) {
                        // 忽略无效的槽位键
                    }
                }
                storageData[guiId] = convertedItems
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.load_guidata_failed", e.message ?: "Unknown error")
        }
    }

    private fun saveStorageData() {
        try {
            FileWriter(storageFile).use { writer ->
                gson.toJson(storageData, writer)
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.load_guidata_failed", e.message ?: "Unknown error")
        }
    }

    /**
     * 将ItemStack转换为Base64字符串
     */
    private fun itemToBase64(item: ItemStack): String? {
        return try {
            val outputStream = ByteArrayOutputStream()
            BukkitObjectOutputStream(outputStream).use { it.writeObject(item) }
            Base64.getEncoder().encodeToString(outputStream.toByteArray())
        } catch (e: Exception) {
            PLog.warning("log.warning.serialize_item_failed", e.message ?: "Unknown error")
            null
        }
    }

    /**
     * 将Base64串转换为ItemStack
     */
    private fun base64ToItem(encoded: String): ItemStack? {
        return try {
            val data = Base64.getDecoder().decode(encoded)
            val inputStream = ByteArrayInputStream(data)
            BukkitObjectInputStream(inputStream).use { it.readObject() as ItemStack }
        } catch (e: Exception) {
            PLog.warning("log.warning.deserialize_item_failed", e.message ?: "Unknown error")
            null
        }
    }
} 