package tech.ezeny.luagin.lua.api

import org.bukkit.Bukkit
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import party.iroiro.luajava.Lua
import party.iroiro.luajava.luajit.LuaJitConsts.LUA_REGISTRYINDEX
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.lua.LuaValueFactory
import tech.ezeny.luagin.protocol.PacketHandler
import tech.ezeny.luagin.protocol.PacketWrapper
import tech.ezeny.luagin.protocol.ProtocolManager
import tech.ezeny.luagin.utils.PLog

object ProtocolAPI : LuaAPIProvider, KoinComponent {
    private lateinit var plugin: Luagin
    private val protocolManager: ProtocolManager by inject()
    private val apiNames = mutableListOf<String>()

    override fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    override fun registerAPI(lua: Lua) {
        // 检查 ProtocolLib 是否可用
        if (!protocolManager.isAvailable) {
            PLog.info("log.info.protocol_api_disabled")
            return
        }

        // 创建 protocol 表
        lua.newTable()

        // protocol.on_receive(packet_type: string, callback: function) - 监听服务端收到的包
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val packetTypeName = luaState.toString(1) ?: ""
            val handlerIndex = 2

            if (!luaState.isFunction(handlerIndex)) {
                return@push 0
            }

            try {
                val packetType = protocolManager.parsePacketType(packetTypeName)

                luaState.pushValue(handlerIndex)
                val functionRef = luaState.ref(LUA_REGISTRYINDEX)

                // 创建包装处理器
                // 协议包处理在异步线程中，Lua 调用在主线程
                val handler = PacketHandler { wrapper, player ->
                    if (Bukkit.isPrimaryThread()) {
                        // 如果已经在主线程，直接调用并返回结果
                        protocolManager.callLuaHandler(luaState, lua, functionRef, wrapper, player)
                    } else {
                        // 在异步线程中，同步调用主线程并等待结果
                        try {
                            val future = Bukkit.getScheduler().callSyncMethod(plugin) {
                                protocolManager.callLuaHandler(luaState, lua, functionRef, wrapper, player)
                            }
                            // 等待主线程执行完成并获取结果
                            future.get()
                        } catch (e: Exception) {
                            PLog.warning("log.warning.received_packet_handler_failed", e.message ?: "Unknown error")
                            true // 出错时不拦截
                        }
                    }
                }

                protocolManager.registerReceivingListener(packetType, handler)
            } catch (e: Exception) {
                PLog.warning("log.warning.invalid_packet_type", packetTypeName)
            }

            return@push 0
        }
        lua.setField(-2, "on_receive")

        // protocol.on_send(packet_type: string, callback: function) - 监听服务端发送的包
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val packetTypeName = luaState.toString(1) ?: ""
            val handlerIndex = 2

            if (!luaState.isFunction(handlerIndex)) {
                return@push 0
            }

            try {
                val packetType = protocolManager.parsePacketType(packetTypeName)

                luaState.pushValue(handlerIndex)
                val functionRef = luaState.ref(LUA_REGISTRYINDEX)

                val handler = PacketHandler { wrapper, player ->
                    if (Bukkit.isPrimaryThread()) {
                        protocolManager.callLuaHandler(luaState, lua, functionRef, wrapper, player)
                    } else {
                        try {
                            val future = Bukkit.getScheduler().callSyncMethod(plugin) {
                                protocolManager.callLuaHandler(luaState, lua, functionRef, wrapper, player)
                            }
                            future.get()
                        } catch (e: Exception) {
                            PLog.warning("log.warning.sent_packet_handler_failed", e.message ?: "Unknown error")
                            true
                        }
                    }
                }

                protocolManager.registerSendingListener(packetType, handler)
            } catch (e: Exception) {
                PLog.warning("log.warning.invalid_packet_type", packetTypeName)
            }

            return@push 0
        }
        lua.setField(-2, "on_send")

        // protocol.send_packet(player_name: string, packet_wrapper: PacketWrapper) - 发送包
        // protocol.send_packet(player_name: string, packet_type: string, data: table)
        lua.push { luaState ->
            if (luaState.top < 2) {
                return@push 0
            }

            val playerName = luaState.toString(1) ?: ""

            // 检查第二个参数是否是 PacketWrapper
            var isPacketWrapper = false
            if (luaState.isTable(2)) {
                luaState.getField(2, "__obj")
                val secondArg = luaState.toJavaObject(-1)
                luaState.pop(1)

                if (secondArg is PacketWrapper) {
                    isPacketWrapper = true
                    val wrapper = secondArg

                    runOnMainThread {
                        try {
                            val player = Bukkit.getPlayerExact(playerName)
                            if (player == null) {
                                PLog.warning("log.warning.player_not_found", playerName)
                                return@runOnMainThread
                            }
                            protocolManager.sendPacket(player, wrapper.getHandle())
                        } catch (e: Exception) {
                            PLog.warning("log.warning.send_packet_failed", e.message ?: "Unknown error")
                        }
                    }
                }
            }

            if (!isPacketWrapper) {
                val packetTypeName = luaState.toString(2) ?: ""

                runOnMainThread {
                    try {
                        val player = Bukkit.getPlayerExact(playerName)
                        if (player == null) {
                            PLog.warning("log.warning.player_not_found", playerName)
                            return@runOnMainThread
                        }

                        val packetType = protocolManager.parsePacketType(packetTypeName)
                        val packet = protocolManager.createPacket(packetType)

                        packet?.let { protocolManager.sendPacket(player, it) }
                    } catch (e: Exception) {
                        PLog.warning("log.warning.send_packet_failed", e.message ?: "Unknown error")
                    }
                }
            }

            return@push 0
        }
        lua.setField(-2, "send_packet")

        // protocol.create_packet(packet_type: string): PacketWrapper - 创建一个新的数据包
        lua.push { luaState ->
            if (luaState.top < 1) {
                luaState.pushNil()
                return@push 1
            }

            val packetTypeName = luaState.toString(1) ?: ""

            try {
                val packetType = protocolManager.parsePacketType(packetTypeName)
                val packet = protocolManager.createPacket(packetType) ?: run {
                    luaState.pushNil()
                    return@push 1
                }

                val dummyPlayer = Bukkit.getOnlinePlayers().firstOrNull()
                if (dummyPlayer != null) {
                    val wrapper = PacketWrapper(packet, dummyPlayer)
                    LuaValueFactory.pushJavaObject(lua, wrapper)
                    return@push 1
                } else {
                    luaState.pushNil()
                    return@push 1
                }
            } catch (e: IllegalArgumentException) {
                PLog.warning("log.warning.invalid_packet_type", packetTypeName)
                luaState.pushNil()
                return@push 1
            } catch (e: Exception) {
                PLog.warning("log.warning.create_packet_failed", packetTypeName, e.message ?: "Unknown error")
                luaState.pushNil()
                return@push 1
            }
        }
        lua.setField(-2, "create_packet")

        // 设置全局变量
        lua.setGlobal("protocol")

        if (!apiNames.contains("protocol")) {
            apiNames.add("protocol")
        }
    }

    override fun getAPINames(): List<String> = apiNames

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }
}
