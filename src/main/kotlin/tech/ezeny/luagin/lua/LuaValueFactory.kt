package tech.ezeny.luagin.lua

import org.bukkit.command.CommandSender
import org.bukkit.event.Event
import party.iroiro.luajava.Lua
import tech.ezeny.luagin.protocol.PacketWrapper
import tech.ezeny.luagin.utils.PLog
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java

object LuaValueFactory {
    // 缓存 Class 到 getter/setter 方法
    private val getterCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()
    private val setterCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    // 缓存 Class 到方法映射
    private val methodCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, List<Method>>>()

    // 缓存 snake_case 到 camelCase 的转换结果
    private val snakeToCamelCache = ConcurrentHashMap<String, String>()

    /**
     * 将 Java 对象推送到 Lua 栈
     */
    fun pushJavaObject(lua: Lua, obj: Any?) {
        when (obj) {
            null -> lua.pushNil()
            is String -> lua.push(obj)
            is Number -> lua.push(obj)
            is Boolean -> lua.push(obj)
            is Collection<*> -> lua.push(obj)
            is Map<*, *> -> lua.push(obj)
            is Enum<*> -> lua.push(obj.name)
            is Event, is CommandSender, is PacketWrapper -> {
                createCustomUserdata(lua, obj)
            }

            else -> {
                val clazz = obj.javaClass
                if (clazz.isPrimitive || clazz.isEnum || isPrimitiveWrapper(clazz) || isSimpleType(clazz)) {
                    lua.push(obj.toString())
                } else {
                    createCustomUserdata(lua, obj)
                }
            }
        }
    }

    /**
     * 从 Lua 栈获取值并转换为 Java 对象
     */
    fun getLuaValue(lua: Lua, index: Int): Any? {
        return when {
            lua.isNil(index) -> null
            lua.isString(index) -> lua.toString(index)
            lua.isBoolean(index) -> lua.toBoolean(index)
            lua.isNumber(index) -> lua.toNumber(index)
            lua.isTable(index) -> lua.toJavaObject(index)
            else -> lua.toJavaObject(index)
        }
    }

    /**
     * 检查是否为基本类型包装类
     */
    private fun isPrimitiveWrapper(clazz: Class<*>): Boolean {
        return clazz in setOf(
            java.lang.Boolean::class.java,
            java.lang.Byte::class.java,
            Character::class.java,
            java.lang.Short::class.java,
            Integer::class.java,
            java.lang.Long::class.java,
            java.lang.Float::class.java,
            java.lang.Double::class.java
        )
    }

    /**
     * 检查是否为简单类型（通过类名判断）
     */
    private fun isSimpleType(clazz: Class<*>): Boolean {
        val simpleName = clazz.simpleName
        return simpleName.endsWith("Type") || simpleName.endsWith("Material")
    }

    /**
     * 为指定的 Java 对象创建自定义 userdata，并设置元表
     */
    private fun createCustomUserdata(lua: Lua, obj: Any) {
        // 创建一个 userdata 来存储对象引用
        lua.newTable()
        lua.pushJavaObject(obj)
        lua.setField(-2, "__obj")

        // 创建元表
        lua.newTable()

        // 设置 __index 元方法
        lua.push { luaState ->
            // 从表中获取原始对象
            luaState.getField(1, "__obj")
            val self = luaState.toJavaObject(-1)
            luaState.pop(1) // 清理栈

            val key = luaState.toString(2) ?: return@push 0

            if (self == null) {
                PLog.warning("log.warning.object_is_null", key)
                luaState.pushNil()
                return@push 1
            }

            // 驼峰命名法和蛇形命名法转换
            val camelKey = snakeToCamel(key)
            val getters = getGetterMethods(self.javaClass)

            // 尝试作为属性访问（getter）
            val getterMethod = getters[key] ?: getters[camelKey]
            if (getterMethod != null) {
                try {
                    val result = getterMethod.invoke(self)
                    pushJavaObject(luaState, result)
                    return@push 1
                } catch (e: Exception) {
                    PLog.warning("log.warning.getter_failed", key, e.message ?: "Unknown error")
                    luaState.pushNil()
                    return@push 1
                }
            }

            // 尝试作为方法访问，返回一个可调用的函数
            val methods = getMethodsByName(self.javaClass, key) + getMethodsByName(self.javaClass, camelKey)
            if (methods.isNotEmpty()) {
                // 返回一个 Lua 函数
                luaState.push { callState ->
                    val totalArgs = callState.top
                    val argCount = if (totalArgs > 0) totalArgs - 1 else 0

                    // 查找匹配的方法
                    val matchingMethod = methods.firstOrNull { it.parameterCount == argCount }

                    if (matchingMethod != null) {
                        try {
                            // 设置可访问性
                            matchingMethod.isAccessible = true
                            
                            val result = if (argCount == 0) {
                                matchingMethod.invoke(self)
                            } else {
                                val args = Array(argCount) { i -> 
                                    coerceLuaToJava(callState, i + 2, matchingMethod.parameterTypes[i])
                                }
                                matchingMethod.invoke(self, *args)
                            }
                            pushJavaObject(callState, result)
                            return@push 1
                        } catch (e: Exception) {
                            PLog.warning("log.warning.method_call_failed", key, e.message ?: "Unknown error")
                            callState.pushNil()
                            return@push 1
                        }
                    } else {
                        PLog.warning("log.warning.no_matching_method", key)
                        callState.pushNil()
                        return@push 1
                    }
                }
                return@push 1
            }

            // 特殊处理 PacketWrapper：尝试转发到 PacketContainer
            if (self is PacketWrapper) {
                try {
                    val packetMethod = PacketWrapper.findPacketMethod(key, 0) 
                        ?: PacketWrapper.findPacketMethod(camelKey, 0)
                    
                    if (packetMethod != null) {
                        // 返回一个 Lua 函数来调用 PacketContainer 的方法
                        luaState.push { callState ->
                            val totalArgs = callState.top
                            val argCount = if (totalArgs > 0) totalArgs - 1 else 0
                            
                            val method = PacketWrapper.findPacketMethod(key, argCount)
                                ?: PacketWrapper.findPacketMethod(camelKey, argCount)
                            
                            if (method != null) {
                                try {
                                    // 设置可访问性
                                    method.isAccessible = true
                                    
                                    val result = if (argCount == 0) {
                                        method.invoke(self.getHandle())
                                    } else {
                                        val args = Array(argCount) { i ->
                                            coerceLuaToJava(callState, i + 2, method.parameterTypes[i])
                                        }
                                        method.invoke(self.getHandle(), *args)
                                    }
                                    pushJavaObject(callState, result)
                                    return@push 1
                                } catch (e: Exception) {
                                    PLog.warning("log.warning.method_call_failed", key, e.message ?: "Unknown error")
                                    callState.pushNil()
                                    return@push 1
                                }
                            } else {
                                callState.pushNil()
                                return@push 1
                            }
                        }
                        return@push 1
                    }
                } catch (e: Exception) {
                    // 忽略错误
                }
            }

            PLog.warning("log.warning.method_not_found", key)
            luaState.pushNil()
            return@push 1
        }
        lua.setField(-2, "__index")

        // 设置 __newindex 元方法
        lua.push { luaState ->
            // 从表中获取原始对象
            luaState.getField(1, "__obj")
            val self = luaState.toJavaObject(-1)
            luaState.pop(1)

            val key = luaState.toString(2) ?: return@push 0

            if (self == null) return@push 0

            // 支持驼峰命名法和蛇形命名法转换
            val camelKey = snakeToCamel(key)
            val setters = getSetterMethods(self.javaClass)

            val setterMethod = setters[key] ?: setters[camelKey]
            if (setterMethod != null) {
                try {
                    val value = coerceLuaToJava(luaState, 3, setterMethod.parameterTypes[0])
                    setterMethod.invoke(self, value)
                } catch (e: Exception) {
                    PLog.warning("log.warning.setter_failed", key, e.message ?: "Unknown error")
                }
            }

            return@push 0
        }
        lua.setField(-2, "__newindex")

        // 设置 __tostring 元方法
        lua.push { luaState ->
            // 从表中获取原始对象
            luaState.getField(1, "__obj")
            val self = luaState.toJavaObject(-1)
            luaState.pop(1)

            luaState.push("JavaObject<${self?.javaClass?.simpleName ?: "null"}>@${self?.hashCode() ?: 0}")
            return@push 1
        }
        lua.setField(-2, "__tostring")

        // 设置元表
        lua.setMetatable(-2)
    }

    /**
     * 提取指定类中符合 getter 规范的方法
     * 映射字段名到对应 Method
     *
     * @param clazz Java 类
     * @return 字段名到 getter 方法的映射
     */
    private fun getGetterMethods(clazz: Class<*>): Map<String, Method> {
        return getterCache.computeIfAbsent(clazz) {
            it.methods
                .filter { method ->
                    method.parameterCount == 0 && (method.name.startsWith("get") || method.name.startsWith("is"))
                }
                .associateBy { method ->
                    method.name.removePrefix("get").removePrefix("is").replaceFirstChar { c -> c.lowercaseChar() }
                }
        }
    }

    /**
     * 提取指定类中符合 setter 规范的方法
     * 映射字段名到对应 Method
     *
     * @param clazz Java 类
     * @return 字段名到 setter 方法的映射
     */
    private fun getSetterMethods(clazz: Class<*>): Map<String, Method> {
        return setterCache.computeIfAbsent(clazz) {
            it.methods
                .filter { method -> method.parameterCount == 1 && method.name.startsWith("set") }
                .associateBy { method -> method.name.removePrefix("set").replaceFirstChar { c -> c.lowercaseChar() } }
        }
    }

    /**
     * 获取指定类中与给定名称匹配的所有方法
     *
     * @param clazz 目标类
     * @param name 方法名
     * @return 匹配的方法列表
     */
    private fun getMethodsByName(clazz: Class<*>, name: String): List<Method> {
        val classMethodCache = methodCache.computeIfAbsent(clazz) { ConcurrentHashMap() }
        return classMethodCache.computeIfAbsent(name) { methodName ->
            clazz.methods.filter { it.name.equals(methodName, ignoreCase = true) }
        }
    }

    /**
     * 将 Lua 值转换为 Java 对象
     */
    private fun coerceLuaToJava(lua: Lua, index: Int, targetType: Class<*>): Any? {
        return when (targetType) {
            Boolean::class.java, Boolean::class.javaPrimitiveType -> lua.toBoolean(index)
            Int::class.java, Int::class.javaPrimitiveType -> lua.toInteger(index).toInt()
            Long::class.java, Long::class.javaPrimitiveType -> lua.toInteger(index)
            Byte::class.java, Byte::class.javaPrimitiveType -> lua.toInteger(index).toByte()
            Short::class.java, Short::class.javaPrimitiveType -> lua.toInteger(index).toShort()
            Float::class.java, Float::class.javaPrimitiveType -> lua.toNumber(index).toFloat()
            Double::class.java, Double::class.javaPrimitiveType -> lua.toNumber(index)
            String::class.java -> lua.toString(index)
            else -> {
                val obj = lua.toJavaObject(index)
                if (obj != null && targetType.isAssignableFrom(obj.javaClass)) obj else null
            }
        }
    }

    /**
     * 将蛇形命名法转换为驼峰命名法
     */
    private fun snakeToCamel(input: String): String {
        return snakeToCamelCache.computeIfAbsent(input) { key ->
            if ('_' !in key) return@computeIfAbsent key

            val result = StringBuilder(key.length)
            var capitalizeNext = false

            for (char in key) {
                when {
                    char == '_' -> capitalizeNext = true
                    capitalizeNext -> {
                        result.append(char.uppercaseChar())
                        capitalizeNext = false
                    }

                    else -> result.append(char)
                }
            }
            result.toString()
        }
    }
}