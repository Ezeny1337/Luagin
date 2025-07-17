package tech.ezeny.luagin.config

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class CacheManager {
    private val cache = ConcurrentHashMap<String, CacheEntry<*>>()
    private val scheduler: ScheduledExecutorService = Executors.newScheduledThreadPool(1)
    private val writeQueue = ConcurrentHashMap<String, MutableList<() -> Unit>>()

    data class CacheEntry<T>(
        val value: T,
        val expireTime: Long,
        val lastUpdate: Long = System.currentTimeMillis()
    )

    @Suppress("UNCHECKED_CAST")
    fun <T> get(key: String): T? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expireTime) {
            cache.remove(key)
            return null
        }
        return (entry as CacheEntry<T>).value
    }

    fun <T> put(key: String, value: T, expireTime: Duration = 5.toDuration(DurationUnit.MINUTES)) {
        val entry = CacheEntry(
            value = value,
            expireTime = System.currentTimeMillis() + expireTime.inWholeMilliseconds
        )
        cache[key] = entry
    }

    fun scheduleWrite(key: String, operation: () -> Unit, delay: Duration = 1.toDuration(DurationUnit.SECONDS)) {
        writeQueue.computeIfAbsent(key) { mutableListOf() }.add(operation)
        
        scheduler.schedule({
            val operations = writeQueue.remove(key) ?: return@schedule
            operations.forEach { it() }
        }, delay.inWholeMilliseconds, TimeUnit.MILLISECONDS)
    }

    fun shutdown() {
        scheduler.shutdown()
        try {
            if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                scheduler.shutdownNow()
            }
        } catch (e: InterruptedException) {
            scheduler.shutdownNow()
        }
    }
} 