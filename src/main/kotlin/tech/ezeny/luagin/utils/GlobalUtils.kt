package tech.ezeny.luagin.utils

import org.bukkit.Bukkit

object GlobalUtils {
    
    /**
     * 获取主世界时间
     *
     * @return 主世界的当前时间（单位为 tick，每天 24000 tick）
     */
    fun getOverworldTime(): Long {
        return try {
            val overworld = Bukkit.getWorlds().find { it.environment == org.bukkit.World.Environment.NORMAL }
            overworld?.time ?: 0L
        } catch (e: Exception) {
            PLog.warning("log.warning.get_overworld_time_failed", e.message ?: "Unknown error")
            0L
        }
    }

    /**
     * 获取服务器 TPS
     * 通过反射访问 MinecraftServer 的 recentTps 字段
     * 可能有兼容性问题 idk
     *
     * @return 一个包含三个 double 值的数组，分别是过去1、5、15分钟的平均 TPS
     */
    fun getServerTPS(): DoubleArray {
        return try {
            // 获取 MinecraftServer 实例
            val server = Bukkit.getServer()
            val getServerMethod = server.javaClass.getMethod("getServer")
            val minecraftServer = getServerMethod.invoke(server)

            // 获取 recentTps 字段
            val recentTpsField = minecraftServer.javaClass.getField("recentTps")
            recentTpsField.isAccessible = true

            // 获取 TPS 数据
            val recentTps = recentTpsField.get(minecraftServer) as DoubleArray

            // 返回3个值（1分钟、5分钟、15分钟）
            when {
                recentTps.size >= 3 -> recentTps.take(3).toDoubleArray()
                recentTps.size == 2 -> doubleArrayOf(recentTps[0], recentTps[1], recentTps[1])
                recentTps.size == 1 -> doubleArrayOf(recentTps[0], recentTps[0], recentTps[0])
                else -> doubleArrayOf(20.0, 20.0, 20.0) // 默认值
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.get_tps_failed", e.message ?: "Unknown error")
            doubleArrayOf(20.0, 20.0, 20.0)
        }
    }
} 