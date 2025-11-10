package tech.ezeny.luagin.protocol

import com.comphenix.protocol.PacketType
import com.comphenix.protocol.ProtocolLibrary
import com.comphenix.protocol.ProtocolManager
import com.comphenix.protocol.events.ListenerPriority
import com.comphenix.protocol.events.PacketAdapter
import com.comphenix.protocol.events.PacketContainer
import com.comphenix.protocol.events.PacketEvent
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.utils.PLog
import java.util.concurrent.ConcurrentHashMap

class ProtocolManager(private val plugin: Luagin) {
    private val protocolManager: ProtocolManager? = if (isProtocolLibAvailable()) {
        ProtocolLibrary.getProtocolManager()
    } else {
        null
    }

    // 存储 Lua 包处理器，按包类型分组
    private val packetHandlers = ConcurrentHashMap<PacketType, MutableList<Pair<String, PacketHandler>>>()

    // 已注册的适配器，用于清理
    private val registeredAdapters = mutableListOf<PacketAdapter>()

    // 当前正在加载的脚本名称
    private var currentScriptName: String = ""

    // ProtocolLib 是否可用
    val isAvailable: Boolean = protocolManager != null

    init {
        if (!isAvailable) {
            PLog.info("log.info.protocol_lib_not_found")
        }
    }

    companion object {
        /**
         * 检查 ProtocolLib 是否可用
         */
        fun isProtocolLibAvailable(): Boolean {
            return try {
                Bukkit.getPluginManager().getPlugin("ProtocolLib") != null
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * 设置当前正在加载的脚本名称
     */
    fun setCurrentScript(scriptName: String) {
        currentScriptName = scriptName
    }

    /**
     * 注册接收包监听器
     */
    fun registerReceivingListener(packetType: PacketType, handler: PacketHandler) {
        registerListener(packetType, handler, true)
    }

    /**
     * 注册发送包监听器
     */
    fun registerSendingListener(packetType: PacketType, handler: PacketHandler) {
        registerListener(packetType, handler, false)
    }

    /**
     * 注册包监听器
     */
    private fun registerListener(packetType: PacketType, handler: PacketHandler, receiving: Boolean) {
        if (protocolManager == null) {
            PLog.warning("log.warning.protocol_lib_not_available")
            return
        }

        // 添加处理器到列表
        val handlers = packetHandlers.getOrPut(packetType) { mutableListOf() }
        handlers.add(Pair(currentScriptName, handler))

        // 如果是第一次注册此包类型，创建适配器
        if (handlers.size == 1) {
            val adapter = object : PacketAdapter(
                plugin,
                ListenerPriority.NORMAL,
                packetType
            ) {
                override fun onPacketReceiving(event: PacketEvent) {
                    if (receiving) {
                        handlePacket(event)
                    }
                }

                override fun onPacketSending(event: PacketEvent) {
                    if (!receiving) {
                        handlePacket(event)
                    }
                }

                private fun handlePacket(event: PacketEvent) {
                    val handlers = packetHandlers[packetType] ?: return
                    val wrapper = PacketWrapper(event.packet, event.player)

                    for ((scriptName, handler) in handlers) {
                        try {
                            val result = handler.handle(wrapper, event.player)
                            if (!result) {
                                event.isCancelled = true
                            }
                        } catch (e: Exception) {
                            PLog.warning("log.warning.handle_packet_failed", scriptName, e.message ?: "Unknown error")
                        }
                    }
                }
            }

            protocolManager.addPacketListener(adapter)
            registeredAdapters.add(adapter)
        }
    }

    /**
     * 发送数据包给玩家
     */
    fun sendPacket(player: Player, packet: PacketContainer) {
        if (protocolManager == null) {
            PLog.warning("log.warning.protocol_lib_not_available")
            return
        }
        try {
            protocolManager.sendServerPacket(player, packet)
        } catch (e: Exception) {
            PLog.warning("log.warning.send_packet_failed", e.message ?: "Unknown error")
        }
    }

    /**
     * 创建新的数据包
     */
    fun createPacket(packetType: PacketType): PacketContainer? {
        if (protocolManager == null) {
            PLog.warning("log.warning.protocol_lib_not_available")
            return null
        }
        return protocolManager.createPacket(packetType)
    }

    /**
     * 清除指定脚本的所有包处理器
     */
    fun clearHandlersForScript(scriptName: String) {
        val typesToRemove = mutableListOf<PacketType>()

        packetHandlers.forEach { (packetType, handlers) ->
            handlers.removeIf { it.first == scriptName }
            if (handlers.isEmpty()) {
                typesToRemove.add(packetType)
            }
        }

        typesToRemove.forEach { packetHandlers.remove(it) }
    }

    /**
     * 清除所有包处理器
     */
    fun clearAllHandlers() {
        packetHandlers.clear()
        if (protocolManager != null) {
            registeredAdapters.forEach { protocolManager.removePacketListener(it) }
        }
        registeredAdapters.clear()
    }

    /**
     * 解析包类型字符串
     */
    fun parsePacketType(typeName: String): PacketType {
        val parts = typeName.split(".")

        return when (parts.size) {
            3 -> {
                val protocol = parts[0]
                val sender = parts[1]
                val name = parts[2]

                PacketType.values().firstOrNull {
                    it.protocol.name.equals(protocol, ignoreCase = true) &&
                            it.sender.name.equals(sender, ignoreCase = true) &&
                            it.name().endsWith(name, ignoreCase = true)
                } ?: throw IllegalArgumentException("Unknown packet type: $typeName")
            }

            else -> throw IllegalArgumentException("Invalid packet type format: $typeName")
        }
    }

    /**
     * 在主线程中调用 Lua 处理器
     */
    fun callLuaHandler(
        luaState: Lua,
        lua: Lua,
        functionRef: Int,
        wrapper: PacketWrapper,
        player: Player
    ): Boolean {
        var result = true
        try {
            // 获取 Lua 函数引用
            luaState.rawGetI(LUA_REGISTRYINDEX, functionRef)

            // 推送 wrapper 对象
            LuaValueFactory.pushJavaObject(lua, wrapper)

            // 推送 player 对象
            LuaValueFactory.pushJavaObject(lua, player)

            // 调用 Lua 函数
            luaState.pCall(2, 1)

            // 获取返回值
            if (luaState.isBoolean(-1)) {
                result = luaState.toBoolean(-1)
            }
            luaState.pop(1)
        } catch (e: Exception) {
            PLog.warning("log.warning.protocol_handler_error", e.message ?: "Unknown error")
        }
        return result
    }
}

/**
 * 包处理器接口
 */
fun interface PacketHandler {
    /**
     * 处理数据包
     * @return true 允许包通过，false 取消包
     */
    fun handle(wrapper: PacketWrapper, player: Player): Boolean
}
