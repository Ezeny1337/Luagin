package tech.ezeny.luagin.performance

import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.scheduler.BukkitTask
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.utils.PLog
import java.io.File
import java.lang.management.ManagementFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import oshi.SystemInfo
import oshi.hardware.CentralProcessor
import oshi.hardware.GlobalMemory
import oshi.hardware.NetworkIF
import oshi.software.os.FileSystem
import oshi.software.os.OperatingSystem

class PerformanceMonitor(private val plugin: Luagin, private val yamlManager: YamlManager) {
    // 是否启用
    @Volatile
    var isEnabled: Boolean = false
        private set

    // 延迟初始化系统信息
    private var systemInfo: SystemInfo? = null

    // 性能数据缓存
    private val performanceCache = ConcurrentHashMap<String, Any>()

    // 服务器数据缓存
    private val serverDataCache = ConcurrentHashMap<String, Any>()

    // 更新频率
    @Volatile
    var serverUpdateInterval: Long = 20L
        private set

    @Volatile
    var systemUpdateInterval: Long = 20L
        private set

    // CPU使用率缓存
    private val cpuUsageCache = ConcurrentHashMap<String, Double>()
    private val lastCpuUpdateTime = AtomicLong(0)
    private val cpuCacheExpiryTime = 500L

    // CPU使用率计算所需的上一时刻数据
    private var previousCpuTicks: LongArray = LongArray(0)
    private var previousCpuTime: Long = 0
    private var previousProcessorTicks: Array<LongArray> = emptyArray()

    // 网络速率缓存
    private var previousNetworkStats: Map<String, NetworkStat> = emptyMap()
    private var previousNetworkTime: Long = 0

    // 磁盘 IO 速率缓存
    private var previousDiskStats: Map<String, Pair<Long, Long>> = emptyMap() // name -> (readBytes, writeBytes)
    private var previousDiskTime: Long = 0

    data class NetworkStat(
        val bytesSent: Long,
        val bytesRecv: Long,
        val packetsSent: Long,
        val packetsRecv: Long
    )

    // 定时任务引用
    private var serverUpdateTask: BukkitTask? = null
    private var systemUpdateTask: BukkitTask? = null

    init {
        initializePerformanceMonitor()
    }

    /**
     * 初始化性能监控系统
     */
    private fun initializePerformanceMonitor() {
        try {
            val config = yamlManager.getConfig("configs/performance.yml") ?: run {
                createDefaultConfig()
                yamlManager.getConfig("configs/performance.yml")
                    ?: throw IllegalStateException("Failed to load performance configuration")
            }

            isEnabled = config.getBoolean("enabled", true)
            if (!isEnabled) {
                PLog.info("log.info.performance_disabled")
                return
            }

            systemInfo = SystemInfo()

            serverUpdateInterval = config.getLong("server_update_interval", 20L).coerceAtLeast(1L)
            systemUpdateInterval = config.getLong("system_update_interval", 20L).coerceAtLeast(1L)

            startPeriodicUpdate()
            PLog.info("log.info.performance_enabled")
        } catch (e: Exception) {
            PLog.severe("log.severe.performance_init_failed", e.message ?: "Unknown error")
            isEnabled = false
        }
    }

    /**
     * 创建默认配置文件
     */
    private fun createDefaultConfig() {
        val defaultConfig = YamlConfiguration().apply {
            set("enabled", true)
            set("server_update_interval", 20L)
            set("system_update_interval", 20L)
        }

        val configFile = File(plugin.dataFolder, "configs/performance.yml")
        configFile.parentFile?.mkdirs()
        defaultConfig.save(configFile)
    }

    /**
     * 启动定期更新任务
     */
    private fun startPeriodicUpdate() {
        if (!isEnabled) return

        // 主线程（更新服务器数据，基于游戏刻）
        serverUpdateTask = Bukkit.getScheduler().runTaskTimer(plugin, Runnable {
            try {
                updateServerData()
            } catch (e: Exception) {
                PLog.warning("log.warning.main_perf_update_failed", e.message ?: "Unknown error")
            }
        }, serverUpdateInterval, serverUpdateInterval)

        // 后台线程（更新 Java 和系统数据，基于真实时间）
        systemUpdateTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, Runnable {
            try {
                updateSystemData()
            } catch (e: Exception) {
                PLog.warning("log.warning.async_perf_update_failed", e.message ?: "Unknown error")
            }
        }, systemUpdateInterval, systemUpdateInterval)
    }

    /**
     * 更新服务器数据更新频率
     * 注意：需要重启服务器才能生效
     * @param intervalTicks 更新间隔（单位：tick）
     */
    fun updateServerUpdateInterval(intervalTicks: Long): Boolean {
        if (!isEnabled) {
            PLog.warning("log.warning.performance_disabled")
            return false
        }

        if (intervalTicks <= 0) {
            PLog.warning("log.warning.invalid_update_interval", intervalTicks.toString())
            return false
        }

        // 更新配置文件
        updateConfigValue("server_update_interval", intervalTicks)
        return true
    }

    /**
     * 更新系统数据更新频率
     * 注意：需要重启服务器才能生效
     * @param intervalTicks 更新间隔（单位：tick）
     */
    fun updateSystemUpdateInterval(intervalTicks: Long): Boolean {
        if (!isEnabled) {
            PLog.warning("log.warning.performance_disabled")
            return false
        }

        if (intervalTicks <= 0) {
            PLog.warning("log.warning.invalid_update_interval", intervalTicks.toString())
            return false
        }

        // 更新配置文件
        updateConfigValue("system_update_interval", intervalTicks)
        return true
    }

    /**
     * 更新配置值
     */
    private fun updateConfigValue(key: String, value: Any) {
        val config = yamlManager.getConfig("configs/performance.yml")
        config?.set(key, value)
        yamlManager.saveConfig("configs/performance.yml")
    }

    /**
     * 更新服务器数据，基于游戏刻
     */
    private fun updateServerData() {
        serverDataCache["server"] = getServerPerformance()
    }

    /**
     * 更新系统/Java数据，基于真实时间
     */
    private fun updateSystemData() {
        val sysInfo = systemInfo ?: return

        // 刷新所有磁盘属性
        sysInfo.hardware.diskStores.forEach { it.updateAttributes() }

        performanceCache["java"] = getJavaPerformance()
        performanceCache["system"] = getSystemPerformance()
    }

    /**
     * 获取服务器性能数据
     */
    private fun getServerPerformance(): Map<String, Any> {
        val server = Bukkit.getServer()
        val worlds = server.worlds

        // 计算实体数量
        val totalEntities = worlds.sumOf { it.entities.size }
        val livingEntities = worlds.sumOf { it.livingEntities.size }

        // 计算区块数量
        val loadedChunks = worlds.sumOf { it.loadedChunks.size }

        // 计算 TPS
        val tps = getServerTPS()

        // 计算世界信息
        val worldInfo = worlds.map { world ->
            mapOf(
                "name" to world.name,
                "type" to world.environment.name,
                "entities" to world.entities.size,
                "living_entities" to world.livingEntities.size,
                "loaded_chunks" to world.loadedChunks.size,
                "players" to world.players.size,
                "time" to world.time,
                "full_time" to world.fullTime,
                "has_storm" to world.hasStorm(),
                "is_thundering" to world.isThundering
            )
        }

        return mapOf(
            "total_entities" to totalEntities,
            "living_entities" to livingEntities,
            "loaded_chunks" to loadedChunks,
            "worlds" to worldInfo,
            "tps" to mapOf(
                "1m" to tps[0],
                "5m" to (tps[1]),
                "15m" to (tps[2])
            ),
            "server_name" to server.name,
            "server_version" to server.version,
            "bukkit_version" to server.bukkitVersion,
            "plugin_count" to server.pluginManager.plugins.size
        )
    }

    /**
     * 获取 Java 性能数据
     */
    private fun getJavaPerformance(): Map<String, Any> {
        val memoryBean = ManagementFactory.getMemoryMXBean()
        val threadBean = ManagementFactory.getThreadMXBean()
        val runtime = Runtime.getRuntime()
        // GC 统计
        val gcBeans = ManagementFactory.getGarbageCollectorMXBeans()
        val gcInfo = gcBeans.associate { bean ->
            bean.name to mapOf(
                "collection_count" to bean.collectionCount,
                "collection_time" to bean.collectionTime
            )
        }

        // 内存信息
        val heapMemoryUsage = memoryBean.heapMemoryUsage
        val nonHeapMemoryUsage = memoryBean.nonHeapMemoryUsage

        // 线程信息
        val threadCount = threadBean.threadCount
        val daemonThreadCount = threadBean.daemonThreadCount
        val peakThreadCount = threadBean.peakThreadCount

        // JVM 信息
        val jvmInfo = mapOf(
            "version" to System.getProperty("java.version"),
            "vendor" to System.getProperty("java.vendor"),
        )

        return mapOf(
            "memory" to mapOf(
                "heap" to mapOf(
                    "used" to heapMemoryUsage.used,
                    "committed" to heapMemoryUsage.committed,
                    "free" to (heapMemoryUsage.max - heapMemoryUsage.used),
                    "max" to heapMemoryUsage.max,
                ),
                "non_heap" to mapOf(
                    "used" to nonHeapMemoryUsage.used,
                    "committed" to nonHeapMemoryUsage.committed,
                ),
                "total" to mapOf(
                    "used" to (heapMemoryUsage.used + nonHeapMemoryUsage.used),
                    "committed" to (heapMemoryUsage.committed + nonHeapMemoryUsage.committed),
                    "free" to (runtime.maxMemory() - heapMemoryUsage.used - nonHeapMemoryUsage.used),
                    "max" to runtime.maxMemory()
                )
            ),
            "threads" to mapOf(
                "count" to threadCount,
                "daemon_count" to daemonThreadCount,
                "peak_count" to peakThreadCount,
            ),
            "jvm" to jvmInfo,
            "uptime" to ManagementFactory.getRuntimeMXBean().uptime,
            "gc" to gcInfo
        )
    }

    /**
     * 获取系统性能数据
     */
    private fun getSystemPerformance(): Map<String, Any> {
        val sysInfo = systemInfo ?: return emptyMap()
        val hardware = sysInfo.hardware
        val operatingSystem = sysInfo.operatingSystem

        // CPU 详细信息
        val processor = hardware.processor
        val cpuInfo = getCpuInfo(processor)

        // 内存详细信息
        val memory = hardware.memory
        val memoryInfo = getMemoryInfo(memory)

        // 磁盘信息
        val fileSystem = operatingSystem.fileSystem
        val diskInfo = getDiskInfo(fileSystem)

        // 网络信息
        val networkIFs = hardware.networkIFs
        val networkInfo = getNetworkInfo(networkIFs)

        // 操作系统信息
        val osInfo = getOsInfo(operatingSystem)

        return mapOf(
            "cpu" to cpuInfo,
            "memory" to memoryInfo,
            "disk" to diskInfo,
            "network" to networkInfo,
            "os" to osInfo
        )
    }

    /**
     * 获取 CPU 详细信息
     */
    private fun getCpuInfo(processor: CentralProcessor): Map<String, Any> {
        val cpuUsage = calculateCpuUsage(processor)
        val perCoreUsage = calculatePerCoreUsage(processor)

        return mapOf(
            "name" to processor.processorIdentifier.name,
            "physical_cpu_count" to processor.physicalProcessorCount,
            "logical_cpu_count" to processor.logicalProcessorCount,
            "max_frequency" to processor.maxFreq,
            "current_frequency" to processor.currentFreq[0], // 取第一个核心的频率作为当前频率
            "context_switches" to processor.contextSwitches,
            "interrupts" to processor.interrupts,
            "available_processors" to Runtime.getRuntime().availableProcessors(),
            "usage_percent" to cpuUsage,
            "per_core_usage" to perCoreUsage,
            "overall_usage" to cpuUsage
        )
    }

    /**
     * 获取内存详细信息
     */
    private fun getMemoryInfo(memory: GlobalMemory): Map<String, Any> {
        // 截断为两位小数（在性能敏感场景下优于 format）
        val memoryUsage = (memory.total - memory.available) * 100.0 / memory.total
        val truncatedUsage = truncate2(memoryUsage.coerceIn(0.0, 100.0))

        return mapOf(
            "total" to memory.total,
            "available" to memory.available,
            "used" to (memory.total - memory.available),
            "usage_percent" to truncatedUsage,
            "virtual_memory" to mapOf(
                "total" to memory.virtualMemory.swapTotal,
                "used" to memory.virtualMemory.swapUsed,
                "available" to (memory.virtualMemory.swapTotal - memory.virtualMemory.swapUsed)
            )
        )
    }

    /**
     * 获取磁盘信息
     */
    private fun getDiskInfo(fileSystem: FileSystem): Map<String, Any> {
        val fileStores = fileSystem.fileStores

        val totalSpace = fileStores.sumOf { it.totalSpace }
        val usableSpace = fileStores.sumOf { it.usableSpace }
        val usedSpace = totalSpace - usableSpace

        val currentTime = System.currentTimeMillis()
        val interval = if (previousDiskTime == 0L) 1000L else (currentTime - previousDiskTime).coerceAtLeast(1L)

        val sysInfo = systemInfo ?: return mapOf(
            "total_space" to totalSpace,
            "used_space" to usedSpace,
            "free_space" to usableSpace,
            "usage_percent" to truncate2(usedSpace.toDouble() / totalSpace.toDouble() * 100.0),
            "stores" to emptyList<Map<String, Any>>()
        )
        val diskStores = sysInfo.hardware.diskStores
        val currentDiskStats = diskStores.associate { disk ->
            disk.name to (disk.readBytes to disk.writeBytes)
        }

        val diskDetails = fileStores.map { store ->
            // 用分区 mountPoint 匹配物理磁盘的 partitionList
            val disk = diskStores.find { d ->
                d.partitions.any { p -> p.mountPoint.equals(store.mount, ignoreCase = true) }
            }
            val prev = if (disk != null) previousDiskStats[disk.name] else null
            val readPerSec =
                if (disk != null && prev != null) (disk.readBytes - prev.first) * 1000.0 / interval else 0.0
            val writePerSec =
                if (disk != null && prev != null) (disk.writeBytes - prev.second) * 1000.0 / interval else 0.0
            mapOf(
                "name" to store.name,
                "mount_point" to store.mount,
                "total" to store.totalSpace,
                "used" to (store.totalSpace - store.usableSpace),
                "free" to store.usableSpace,
                "usage_percent" to truncate2(
                    ((store.totalSpace - store.usableSpace) * 100.0 / store.totalSpace).coerceIn(
                        0.0,
                        100.0
                    )
                ),
                "read_bytes_per_sec" to readPerSec,
                "write_bytes_per_sec" to writePerSec
            )
        }

        previousDiskStats = currentDiskStats
        previousDiskTime = currentTime

        return mapOf(
            "total_space" to totalSpace,
            "used_space" to usedSpace,
            "free_space" to usableSpace,
            "usage_percent" to truncate2(usedSpace.toDouble() / totalSpace.toDouble() * 100.0),
            "stores" to diskDetails
        )
    }

    /**
     * 获取网络信息
     */
    private fun getNetworkInfo(networkIFs: List<NetworkIF>): Map<String, Any> {
        val currentTime = System.currentTimeMillis()
        val interval = if (previousNetworkTime == 0L) 1000L else (currentTime - previousNetworkTime).coerceAtLeast(1L)

        val currentStats = networkIFs.associateBy({ it.name }, {
            NetworkStat(it.bytesSent, it.bytesRecv, it.packetsSent, it.packetsRecv)
        })

        // 统计所有网卡的累计数据
        val totalBytesSent = networkIFs.sumOf { it.bytesSent }
        val totalBytesRecv = networkIFs.sumOf { it.bytesRecv }
        val totalPacketsSent = networkIFs.sumOf { it.packetsSent }
        val totalPacketsRecv = networkIFs.sumOf { it.packetsRecv }

        // 统计所有网卡的速率
        var prevTotalBytesSent = 0L
        var prevTotalBytesRecv = 0L
        var prevTotalPacketsSent = 0L
        var prevTotalPacketsRecv = 0L
        if (previousNetworkStats.isNotEmpty()) {
            prevTotalBytesSent = previousNetworkStats.values.sumOf { it.bytesSent }
            prevTotalBytesRecv = previousNetworkStats.values.sumOf { it.bytesRecv }
            prevTotalPacketsSent = previousNetworkStats.values.sumOf { it.packetsSent }
            prevTotalPacketsRecv = previousNetworkStats.values.sumOf { it.packetsRecv }
        }
        val bytesSentPerSec = ((totalBytesSent - prevTotalBytesSent) * 1000.0 / interval).toLong()
        val bytesRecvPerSec = ((totalBytesRecv - prevTotalBytesRecv) * 1000.0 / interval).toLong()
        val packetsSentPerSec = ((totalPacketsSent - prevTotalPacketsSent) * 1000.0 / interval).toLong()
        val packetsRecvPerSec = ((totalPacketsRecv - prevTotalPacketsRecv) * 1000.0 / interval).toLong()

        // Socket 连接数统计（所有TCP连接数）
        val sysInfo = systemInfo ?: return mapOf(
            "total_bytes_sent" to totalBytesSent,
            "total_bytes_recv" to totalBytesRecv,
            "total_packets_sent" to totalPacketsSent,
            "total_packets_recv" to totalPacketsRecv,
            "bytes_sent_per_sec" to bytesSentPerSec,
            "bytes_recv_per_sec" to bytesRecvPerSec,
            "packets_sent_per_sec" to packetsSentPerSec,
            "packets_recv_per_sec" to packetsRecvPerSec,
            "socket_connection_count" to 0L
        )
        val os = sysInfo.operatingSystem
        val ipStats = os.internetProtocolStats
        val tcpv4Stats = ipStats.tcPv4Stats
        val tcpv6Stats = ipStats.tcPv6Stats
        val socketConnectionCount = tcpv4Stats.connectionsEstablished + tcpv6Stats.connectionsEstablished

        // 更新缓存
        previousNetworkStats = currentStats
        previousNetworkTime = currentTime

        return mapOf(
            "total_bytes_sent" to totalBytesSent,
            "total_bytes_recv" to totalBytesRecv,
            "total_packets_sent" to totalPacketsSent,
            "total_packets_recv" to totalPacketsRecv,
            "bytes_sent_per_sec" to bytesSentPerSec,
            "bytes_recv_per_sec" to bytesRecvPerSec,
            "packets_sent_per_sec" to packetsSentPerSec,
            "packets_recv_per_sec" to packetsRecvPerSec,
            "socket_connection_count" to socketConnectionCount
        )
    }

    /**
     * 获取操作系统信息
     */
    private fun getOsInfo(operatingSystem: OperatingSystem): Map<String, Any> {
        val versionInfo = operatingSystem.versionInfo

        val osType = operatingSystem.family
        val bitness = operatingSystem.bitness

        val detailedVersion = "$osType $versionInfo ${bitness}Bit"

        return mapOf(
            "os_version" to detailedVersion,
            "process_count" to operatingSystem.processCount,
            "thread_count" to operatingSystem.threadCount,
            "uptime" to operatingSystem.systemUptime
        )
    }

    /**
     * 获取所有性能数据
     */
    fun getAllPerformanceData(): Map<String, Any> {
        if (!isEnabled) {
            return mapOf(
                "timestamp" to System.currentTimeMillis(),
                "enabled" to false,
                "data" to emptyMap<String, Any>()
            )
        }

        val currentTime = System.currentTimeMillis()

        // 合并所有数据
        val allData = mutableMapOf<String, Any>()
        allData.putAll(serverDataCache)
        allData.putAll(performanceCache)

        return mapOf(
            "timestamp" to currentTime,
            "enabled" to true,
            "data" to allData
        )
    }

    /**
     * 获取特定类型的性能数据
     */
    @Suppress("UNCHECKED_CAST")
    fun getPerformanceData(type: String): Map<String, Any>? {
        if (!isEnabled) return null

        return when (type) {
            "server" -> {
                val data = serverDataCache[type]
                if (data is Map<*, *>) {
                    data as Map<String, Any>
                } else null
            }

            "java", "system" -> {
                val data = performanceCache[type]
                if (data is Map<*, *>) {
                    data as Map<String, Any>
                } else null
            }

            else -> null
        }
    }

    /**
     * 获取性能监控配置信息
     */
    fun getPerformanceConfig(): Map<String, Any> {
        val config = yamlManager.getConfig("configs/performance.yml")

        return if (config != null) {
            val enabled = config.getBoolean("enabled", true)
            val serverInterval = config.getInt("server_update_interval", 20)
            val systemInterval = config.getInt("system_update_interval", 20)

            mapOf(
                "enabled" to enabled,
                "server_update_interval" to serverInterval,
                "system_update_interval" to systemInterval
            )
        } else {
            mapOf(
                "enabled" to true,
                "server_update_interval" to 20,
                "system_update_interval" to 20
            )
        }
    }



    /**
     * 清理性能数据缓存
     */
    fun clearCache() {
        performanceCache.clear()
        serverDataCache.clear()
        cpuUsageCache.clear()
        lastCpuUpdateTime.set(0)
        previousCpuTicks = LongArray(0)
        previousCpuTime = 0
        previousProcessorTicks = emptyArray()
        previousNetworkStats = emptyMap()
        previousNetworkTime = 0
        previousDiskStats = emptyMap()
        previousDiskTime = 0
    }

    /**
     * 停止所有定时任务
     */
    fun shutdown() {
        serverUpdateTask?.cancel()
        systemUpdateTask?.cancel()
        clearCache()
    }

    /**
     * 计算总体 CPU 使用率
     */
    private fun calculateCpuUsage(processor: CentralProcessor): Double {
        val currentTime = System.currentTimeMillis()

        // 检查缓存是否过期
        if (currentTime - lastCpuUpdateTime.get() < cpuCacheExpiryTime) {
            return cpuUsageCache["overall"] ?: 0.0
        }

        val currentTicks = processor.systemCpuLoadTicks

        if (previousCpuTicks.isEmpty()) {
            // 初始化数据
            previousCpuTicks = currentTicks.clone()
            previousCpuTime = currentTime
            return 0.0
        }

        val timeDiff = currentTime - previousCpuTime
        // 时间间隔太短，返回缓存值
        if (timeDiff < 100) {
            return cpuUsageCache["overall"] ?: 0.0
        }

        // 计算 CPU 使用率
        val totalDiff = currentTicks.sum() - previousCpuTicks.sum()
        val idleDiff =
            currentTicks[CentralProcessor.TickType.IDLE.index] - previousCpuTicks[CentralProcessor.TickType.IDLE.index]
        val iowaitDiff =
            currentTicks[CentralProcessor.TickType.IOWAIT.index] - previousCpuTicks[CentralProcessor.TickType.IOWAIT.index]

        val usedDiff = totalDiff - idleDiff - iowaitDiff
        val cpuUsage = if (totalDiff > 0) (usedDiff.toDouble() / totalDiff.toDouble()) * 100.0 else 0.0

        // 更新缓存数据
        previousCpuTicks = currentTicks.clone()
        previousCpuTime = currentTime

        // 更新 CPU 使用率缓存
        val finalUsage = truncate2(cpuUsage.coerceIn(0.0, 100.0))
        cpuUsageCache["overall"] = finalUsage
        lastCpuUpdateTime.set(currentTime)

        return finalUsage
    }

    /**
     * 计算每个核心的 CPU 使用率
     */
    private fun calculatePerCoreUsage(processor: CentralProcessor): DoubleArray {
        val logicalProcessorCount = processor.logicalProcessorCount
        val perCoreUsage = DoubleArray(logicalProcessorCount)

        try {
            // 获取当前处理器的 tick 数据
            val currentProcessorTicks = processor.processorCpuLoadTicks

            if (previousProcessorTicks.isEmpty()) {
                // 初始化数据
                previousProcessorTicks = currentProcessorTicks.clone()
                perCoreUsage.fill(0.0)
                return perCoreUsage
            }

            val processorCpuLoadBetweenTicks = processor.getProcessorCpuLoadBetweenTicks(previousProcessorTicks)

            for (i in 0 until logicalProcessorCount) {
                val usage = if (i < processorCpuLoadBetweenTicks.size) {
                    processorCpuLoadBetweenTicks[i] * 100.0
                } else {
                    0.0
                }
                perCoreUsage[i] = truncate2(usage.coerceIn(0.0, 100.0))
            }

            // 更新缓存数据
            previousProcessorTicks = currentProcessorTicks.clone()

        } catch (e: Exception) {
            PLog.warning("log.warning.get_per_cpu_failed", e.message ?: "Unknown error")
            val overallUsage = calculateCpuUsage(processor)
            perCoreUsage.fill(overallUsage)
        }

        return perCoreUsage
    }

    /**
     * 获取服务器 TPS
     * 通过反射访问 MinecraftServer 的 recentTps 字段
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
                recentTps.size >= 3 -> doubleArrayOf(
                    truncate2(recentTps[0]),
                    truncate2(recentTps[1]),
                    truncate2(recentTps[2])
                )

                recentTps.size == 2 -> doubleArrayOf(
                    truncate2(recentTps[0]),
                    truncate2(recentTps[1]),
                    truncate2(recentTps[1])
                )

                recentTps.size == 1 -> doubleArrayOf(
                    truncate2(recentTps[0]),
                    truncate2(recentTps[0]),
                    truncate2(recentTps[0])
                )

                else -> doubleArrayOf(20.0, 20.0, 20.0) // 默认值
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.get_tps_failed", e.message ?: "Unknown error")
            doubleArrayOf(20.0, 20.0, 20.0)
        }
    }

    private fun truncate2(value: Double): Double = (value * 100).toInt() / 100.0
} 