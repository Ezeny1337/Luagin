package tech.ezeny.luagin.protocol

import com.comphenix.protocol.events.PacketContainer
import org.bukkit.entity.Player
import java.lang.reflect.Method

class PacketWrapper(
    private val packet: PacketContainer,
    val player: Player
) {

    fun getHandle(): PacketContainer = packet

    // 动态方法转发到 PacketContainer
    companion object {
        // 缓存 PacketContainer 的所有方法
        private val packetMethods: Map<String, List<Method>> by lazy {
            PacketContainer::class.java.methods
                .groupBy { it.name }
        }

        // 查找 PacketContainer 中匹配的方法
        fun findPacketMethod(methodName: String, argCount: Int): Method? {
            return packetMethods[methodName]?.firstOrNull { it.parameterCount == argCount }
        }
    }
}
