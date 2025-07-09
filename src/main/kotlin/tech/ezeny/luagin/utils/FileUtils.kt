package tech.ezeny.luagin.utils

import tech.ezeny.luagin.Luagin
import java.io.File
import java.io.IOException

object FileUtils {
    private lateinit var plugin: Luagin

    fun initialize(plugin: Luagin) {
        this.plugin = plugin
    }

    /**
     * 获取基于插件数据目录的绝对路径
     *
     * @param relativePath 相对路径
     * @return 绝对路径
     * @throws IllegalArgumentException 如果路径不安全
     */
    fun getAbsolutePath(relativePath: String): String {
        val normalizedPath = normalizePath(relativePath)
        val file = File(normalizedPath)

        // 检查是否为绝对路径
        if (file.isAbsolute) {
            throw IllegalArgumentException("Absolute paths are not allowed: $relativePath")
        }

        // 检查路径遍历攻击
        if (containsPathTraversal(normalizedPath)) {
            throw IllegalArgumentException("Path traversal is not allowed: $relativePath")
        }

        return File(plugin.dataFolder, normalizedPath).absolutePath
    }

    /**
     * 获取基于插件数据目录的文件对象
     *
     * @param relativePath 相对路径
     * @return File 对象
     * @throws IllegalArgumentException 如果路径不安全
     */
    fun getFile(relativePath: String): File {
        return File(getAbsolutePath(relativePath))
    }

    /**
     * 检查文件是否存在
     *
     * @param relativePath 相对路径
     * @return 文件是否存在
     */
    fun exists(relativePath: String): Boolean {
        return try {
            getFile(relativePath).exists()
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * 创建目录
     *
     * @param relativePath 相对路径
     * @return 是否创建成功
     */
    fun createDirectory(relativePath: String): Boolean {
        return try {
            getFile(relativePath).mkdirs()
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * 创建文件
     *
     * @param relativePath 相对路径
     * @return 是否创建成功
     */
    fun createFile(relativePath: String): Boolean {
        return try {
            val file = getFile(relativePath)
            if (file.exists()) {
                return true
            }

            // 确保父目录存在
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }

            file.createNewFile()
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 读取文件内容
     *
     * @param relativePath 相对路径
     * @return 文件内容，如果文件不存在或读取失败返回 null
     */
    fun readFile(relativePath: String): String? {
        return try {
            val file = getFile(relativePath)
            if (!file.exists() || !file.isFile) {
                return null
            }
            file.readText(Charsets.UTF_8)
        } catch (e: IllegalArgumentException) {
            null
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 写入文件内容
     *
     * @param relativePath 相对路径
     * @param content 文件内容
     * @param isBinary 是否为二进制内容
     * @return 是否写入成功
     */
    fun writeFile(relativePath: String, content: String, isBinary: Boolean = false): Boolean {
        return try {
            val file = getFile(relativePath)

            // 确保父目录存在
            val parent = file.parentFile
            if (parent != null && !parent.exists()) {
                parent.mkdirs()
            }

            if (isBinary) {
                file.writeBytes(content.toByteArray())
            } else {
                file.writeText(content, Charsets.UTF_8)
            }
            true
        } catch (e: IllegalArgumentException) {
            false
        } catch (e: IOException) {
            false
        }
    }

    /**
     * 列出目录内容
     *
     * @param relativePath 相对路径
     * @return 目录内容列表，如果目录不存在或读取失败返回 null
     */
    fun listDirectory(relativePath: String): List<String>? {
        return try {
            val dir = getFile(relativePath)
            if (!dir.exists() || !dir.isDirectory) {
                return null
            }

            val files = dir.listFiles()
            files?.map { it.name } ?: emptyList()
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    /**
     * 删除文件或目录
     *
     * @param relativePath 相对路径
     * @return 是否删除成功
     */
    fun delete(relativePath: String): Boolean {
        return try {
            getFile(relativePath).deleteRecursively()
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * 规范化路径
     * 将路径分隔符统一为系统分隔符
     *
     * @param path 原始路径
     * @return 规范化后的路径
     */
    private fun normalizePath(path: String): String {
        return path.replace("/", File.separator).replace("\\", File.separator)
    }

    /**
     * 检查是否包含路径遍历攻击
     * 检查路径中是否包含 ".." 等危险字符
     *
     * @param path 路径
     * @return 是否包含路径遍历
     */
    private fun containsPathTraversal(path: String): Boolean {
        val normalized = normalizePath(path)
        return normalized.contains("..") ||
                normalized.startsWith("/") ||
                normalized.startsWith("\\") ||
                normalized.contains(":") // Windows 驱动器标识符
    }
}