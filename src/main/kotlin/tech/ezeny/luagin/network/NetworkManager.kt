package tech.ezeny.luagin.network

import org.bukkit.Bukkit
import tech.ezeny.luagin.Luagin
import tech.ezeny.luagin.utils.PLog
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors

class NetworkManager(private val plugin: Luagin) {
    // 创建一个线程池用于异步请求
    private val executor = Executors.newFixedThreadPool(4)

    /**
     * 执行 GET 请求
     *
     * @param urlString 请求 URL
     * @param headers 请求头
     * @param callback 回调函数，接收响应结果
     * @return 如果为同步调用，返回响应内容字符串；否则返回 null
     */
    fun doGetRequest(urlString: String, headers: Map<String, String>? = null, callback: ((String) -> Unit)? = null): String? {
        if (callback != null) {
            // 异步执行
            executor.execute {
                val response = executeGetRequest(urlString, headers)
                runOnMainThread {
                    callback.invoke(response)
                }
            }

            return null
        } else {
            // 同步执行
            return executeGetRequest(urlString, headers)
        }
    }

    /**
     * 执行 POST 请求
     *
     * @param urlString 请求 URL
     * @param data 请求体
     * @param headers 请求头
     * @param callback 回调函数，接收响应结果
     * @return 如果为同步调用，返回响应内容字符串；否则返回 null
     */
    fun doPostRequest(urlString: String, data: Map<String, Any>? = null, headers: Map<String, String>? = null, callback: ((String) -> Unit)? = null): String? {
        if (callback != null) {
            // 异步执行
            executor.execute {
                val response = executePostRequest(urlString, data, headers)
                runOnMainThread {
                    callback.invoke(response)
                }
            }

            return null
        } else {
            // 同步执行
            return executePostRequest(urlString, data, headers)
        }
    }

    /**
     * 执行 GET 请求的实际实现
     * @param urlString 请求 URL
     * @param headers 请求头
     * @return 响应内容字符串
     */
    private fun executeGetRequest(urlString: String, headers: Map<String, String>?): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            // 设置请求头
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            // 获取响应
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = readResponse(connection)
                connection.disconnect()
                response
            } else {
                connection.disconnect()
                "Error: HTTP $responseCode"
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.network_request_failed", urlString, e.message ?: "Unknown error")
            " ${e.message}"
        }
    }

    /**
     * 执行 POST 请求的实际实现
     *
     * @param urlString 请求 URL
     * @param data 请求体
     * @param headers 请求头
     * @return 响应内容字符串
     */
    private fun executePostRequest(urlString: String, data: Map<String, Any>?, headers: Map<String, String>?): String {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.doOutput = true

            // 设置请求头
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            headers?.forEach { (key, value) ->
                connection.setRequestProperty(key, value)
            }

            // 写入请求体
            if (data != null) {
                val postData = convertMapToJson(data)
                DataOutputStream(connection.outputStream).use { it.write(postData.toByteArray(StandardCharsets.UTF_8)) }
            }

            // 获取响应
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val response = readResponse(connection)
                connection.disconnect()
                response
            } else {
                connection.disconnect()
                "Error: HTTP $responseCode"
            }
        } catch (e: Exception) {
            PLog.warning("log.warning.network_request_failed", urlString, e.message ?: "Unknown error")
            "${e.message}"
        }
    }

    /**
     * 读取响应内容
     *
     * @param connection 已连接的 HttpURLConnection
     * @return 响应内容字符串
     */
    private fun readResponse(connection: HttpURLConnection): String {
        val reader = BufferedReader(InputStreamReader(connection.inputStream))
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line)
        }
        reader.close()
        return response.toString()
    }

    private fun convertMapToJson(map: Map<String, Any>): String {
        val json = StringBuilder("{")
        var first = true

        map.forEach { (key, value) ->
            if (!first) json.append(",")
            first = false

            json.append("\"").append(key).append("\"").append(":")

            when (value) {
                is Map<*, *> -> json.append(convertMapToJson(value as Map<String, Any>))
                is String -> json.append("\"").append(value).append("\"")
                is Boolean -> json.append(value)
                is Number -> json.append(value)
                else -> json.append("\"").append(value.toString()).append("\"")
            }
        }

        json.append("}")
        return json.toString()
    }

    private fun runOnMainThread(runnable: Runnable) {
        Bukkit.getScheduler().runTask(plugin, runnable)
    }

    fun convertLuaTableToMap(table: Map<*, *>?): Map<String, Any> {
        if (table == null) return emptyMap()

        val result = mutableMapOf<String, Any>()

        table.forEach { (key, value) ->
            val keyStr = key?.toString() ?: return@forEach

            result[keyStr] = when (value) {
                is Map<*, *> -> convertLuaTableToMap(value)
                is String -> value
                is Boolean -> value
                is Number -> value
                else -> value?.toString() ?: ""
            }
        }

        return result
    }
}