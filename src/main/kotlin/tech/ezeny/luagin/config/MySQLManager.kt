package tech.ezeny.luagin.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.io.File
import java.sql.Connection
import java.sql.SQLException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.time.DurationUnit
import kotlin.time.toDuration

class MySQLManager(private val plugin: Luagin, private val yamlManager: YamlManager) {
    // Hikari 数据源
    private var dataSource: HikariDataSource? = null

    // 缓存管理器，用于缓存查询结果
    private val cacheManager = CacheManager()

    // 用于异步执行数据库操作的线程池
    private val executor = Executors.newFixedThreadPool(4)

    // MySQL 是否启用
    private var isEnabled = false

    init {
        initializeDatabase()
    }

    private fun initializeDatabase() {
        try {
            val config = yamlManager.getConfig("configs/mysql.yml") ?: run {
                createDefaultConfig()
                yamlManager.getConfig("configs/mysql.yml")
                    ?: throw IllegalStateException("Failed to load MySQL configuration")
            }

            isEnabled = config.getBoolean("enabled", false)
            if (!isEnabled) {
                PLog.info("log.info.mysql_disabled")
                return
            }

            val hikariConfig = HikariConfig().apply {
                jdbcUrl = "jdbc:mysql://${config.getString("host", "localhost")}:${
                    config.getInt(
                        "port",
                        3306
                    )
                }/${config.getString("database", "luagin")}"
                username = config.getString("username", "root")
                password = config.getString("password", "")
                maximumPoolSize = config.getInt("pool-size", 10)
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
                addDataSourceProperty("useServerPrepStmts", "true")
                addDataSourceProperty("useLocalSessionState", "true")
                addDataSourceProperty("rewriteBatchedStatements", "true")
                addDataSourceProperty("cacheResultSetMetadata", "true")
                addDataSourceProperty("cacheServerConfiguration", "true")
                addDataSourceProperty("elideSetAutoCommits", "true")
                addDataSourceProperty("maintainTimeStats", "false")
            }

            dataSource = HikariDataSource(hikariConfig)
            testConnection()
            PLog.info("log.info.mysql_connected")
        } catch (e: Exception) {
            PLog.severe("log.severe.mysql_connection_failed", e.message ?: "Unknown error")
        }
    }

    private fun createDefaultConfig() {
        val defaultConfig = YamlConfiguration().apply {
            set("enabled", false)
            set("host", "localhost")
            set("port", 3306)
            set("database", "luagin")
            set("username", "root")
            set("password", "")
            set("pool-size", 10)
        }

        val configFile = File(plugin.dataFolder, "configs/mysql.yml")
        defaultConfig.save(configFile)
    }

    private fun testConnection() {
        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute("SELECT 1")
            }
        }
    }

    fun getConnection(): Connection {
        return dataSource?.connection ?: throw SQLException("Database connection is not initialized")
    }

    fun close() {
        dataSource?.close()
        dataSource = null
        cacheManager.shutdown()
        executor.shutdown()
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow()
            }
        } catch (e: InterruptedException) {
            executor.shutdownNow()
        }
    }

    /**
     * 创建数据库表
     *
     * @param tableName 表名
     * @param columns 列名和列类型的映射
     */
    fun createTable(tableName: String, columns: Map<String, String>) {
        if (!isEnabled) {
            PLog.info("log.info.mysql_disabled")
            return
        }

        val columnDefinitions = columns.entries.joinToString(",\n") { (name, type) ->
            "$name $type"
        }

        val sql = """
            CREATE TABLE IF NOT EXISTS $tableName (
                id INT AUTO_INCREMENT PRIMARY KEY,
                $columnDefinitions
            )
        """.trimIndent()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.execute(sql)
            }
        }
    }

    /**
     * 异步插入数据
     *
     * @param tableName 表名
     * @param values 插入的值，键为列名，值为对应的列值
     * @param callback 插入成功后的回调，返回插入的 ID
     */
    fun insert(tableName: String, values: Map<String, Any>, callback: ((Int) -> Unit)? = null) {
        if (!isEnabled) {
            PLog.info("log.info.mysql_disabled")
            callback?.invoke(-1)
            return
        }

        val cacheKey = "$tableName:${values.values.joinToString(":")}"

        // 先更新缓存
        cacheManager.put(cacheKey, values)

        // 异步执行数据库操作
        executor.execute {
            try {
                val columns = values.keys.joinToString(", ")
                val placeholders = values.keys.joinToString(", ") { "?" }
                val sql = "INSERT INTO $tableName ($columns) VALUES ($placeholders)"

                getConnection().use { connection ->
                    connection.prepareStatement(sql, java.sql.Statement.RETURN_GENERATED_KEYS).use { statement ->
                        values.values.forEachIndexed { index, value ->
                            when (value) {
                                is String -> statement.setString(index + 1, value)
                                is Int -> statement.setInt(index + 1, value)
                                is Double -> statement.setDouble(index + 1, value)
                                is Boolean -> statement.setBoolean(index + 1, value)
                                else -> statement.setString(index + 1, value.toString())
                            }
                        }
                        statement.executeUpdate()

                        statement.generatedKeys.use { keys ->
                            if (keys.next()) {
                                val id = keys.getInt(1)
                                callback?.invoke(id)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                PLog.warning("log.warning.mysql_insert_failed", tableName, e.message ?: "Unknown error")
                callback?.invoke(-1)
            }
        }
    }

    /**
     * 更新数据
     *
     * @param tableName 表名
     * @param values 要更新的列和值
     * @param where 更新条件
     * @param whereArgs 更新条件的参数
     * @param callback 更新后的回调，返回受影响的行数
     */
    fun update(
        tableName: String,
        values: Map<String, Any>,
        where: String,
        whereArgs: List<Any>,
        callback: ((Int) -> Unit)? = null
    ) {
        if (!isEnabled) {
            PLog.info("log.info.mysql_disabled")
            callback?.invoke(-1)
            return
        }

        val cacheKey = "$tableName:$where:${whereArgs.joinToString(":")}"

        // 先更新缓存
        cacheManager.put(cacheKey, values)

        // 延迟写入数据库
        cacheManager.scheduleWrite(cacheKey, {
            try {
                val setClause = values.keys.joinToString(", ") { "$it = ?" }
                val sql = "UPDATE $tableName SET $setClause WHERE $where"

                getConnection().use { connection ->
                    connection.prepareStatement(sql).use { statement ->
                        var index = 1
                        values.values.forEach { value ->
                            when (value) {
                                is String -> statement.setString(index++, value)
                                is Int -> statement.setInt(index++, value)
                                is Double -> statement.setDouble(index++, value)
                                is Boolean -> statement.setBoolean(index++, value)
                                else -> statement.setString(index++, value.toString())
                            }
                        }
                        whereArgs.forEach { arg ->
                            when (arg) {
                                is String -> statement.setString(index++, arg)
                                is Int -> statement.setInt(index++, arg)
                                is Double -> statement.setDouble(index++, arg)
                                is Boolean -> statement.setBoolean(index++, arg)
                                else -> statement.setString(index++, arg.toString())
                            }
                        }
                        val affectedRows = statement.executeUpdate()
                        callback?.invoke(affectedRows)
                    }
                }
            } catch (e: Exception) {
                PLog.warning("log.warning.mysql_update_failed", tableName, e.message ?: "Unknown error")
                callback?.invoke(-1)
            }
        })
    }

    /**
     * 异步查询数据
     *
     * @param tableName 表名
     * @param columns 查询的列
     * @param where 查询的条件
     * @param whereArgs 查询条件的参数
     * @param callback 查询结果的回调，返回查询到的结果集
     */
    fun query(
        tableName: String,
        columns: List<String> = listOf("*"),
        where: String? = null,
        whereArgs: List<Any> = emptyList(),
        callback: ((List<Map<String, Any>>) -> Unit)? = null
    ) {
        if (!isEnabled) {
            PLog.info("log.info.mysql_disabled")
            callback?.invoke(emptyList())
            return
        }

        val cacheKey = "$tableName:${columns.joinToString(":")}:$where:${whereArgs.joinToString(":")}"

        // 先检查缓存
        val cachedResult: List<Map<String, Any>>? = cacheManager.get(cacheKey)
        if (cachedResult != null) {
            callback?.invoke(cachedResult)
            return
        }

        // 异步执行数据库查询
        executor.execute {
            try {
                val columnList = columns.joinToString(", ")
                val whereClause = where?.let { "WHERE $it" } ?: ""
                val sql = "SELECT $columnList FROM $tableName $whereClause"

                val results = mutableListOf<Map<String, Any>>()

                getConnection().use { connection ->
                    connection.prepareStatement(sql).use { statement ->
                        whereArgs.forEachIndexed { index, arg ->
                            when (arg) {
                                is String -> statement.setString(index + 1, arg)
                                is Int -> statement.setInt(index + 1, arg)
                                is Double -> statement.setDouble(index + 1, arg)
                                is Boolean -> statement.setBoolean(index + 1, arg)
                                else -> statement.setString(index + 1, arg.toString())
                            }
                        }

                        statement.executeQuery().use { resultSet ->
                            val metaData = resultSet.metaData
                            while (resultSet.next()) {
                                val row = mutableMapOf<String, Any>()
                                for (i in 1..metaData.columnCount) {
                                    val columnName = metaData.getColumnName(i)
                                    row[columnName] = when (metaData.getColumnType(i)) {
                                        java.sql.Types.INTEGER -> resultSet.getInt(i)
                                        java.sql.Types.DOUBLE -> resultSet.getDouble(i)
                                        java.sql.Types.BOOLEAN -> resultSet.getBoolean(i)
                                        else -> resultSet.getString(i)
                                    }
                                }
                                results.add(row)
                            }
                        }
                    }
                }

                // 更新缓存
                cacheManager.put(cacheKey, results, 1.toDuration(DurationUnit.MINUTES))
                callback?.invoke(results)
            } catch (e: Exception) {
                PLog.warning("log.warning.mysql_query_failed", tableName, e.message ?: "Unknown error")
                callback?.invoke(emptyList())
            }
        }
    }
}