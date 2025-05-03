package tech.ezeny.luagin.lua

import org.bukkit.command.CommandSender
import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import tech.ezeny.luagin.utils.PLog
import java.lang.reflect.Method
import java.util.WeakHashMap
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.java

object LuaValueFactory {
    // 缓存对象到 LuaValue 的映射
    private val objectLuaCache = WeakHashMap<Any, LuaValue>()

    // 缓存 Class 到 Metatable 的映射
    private val metatableCache = ConcurrentHashMap<Class<*>, LuaTable>()

    // 缓存 Class 到 getter/setter 方法
    private val getterCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()
    private val setterCache = ConcurrentHashMap<Class<*>, Map<String, Method>>()

    // 缓存 Class 到方法映射
    private val methodCache = ConcurrentHashMap<Class<*>, ConcurrentHashMap<String, List<Method>>>()

    // 缓存 snake_case 到 camelCase 的转换结果
    private val snakeToCamelCache = ConcurrentHashMap<String, String>()

    /**
     * 将 Java 对象转换为 LuaValue
     *
     * @param obj 需要转换的 Java 对象
     * @return 转换后的 LuaValue
     */
    fun createLuaValue(obj: Any?): LuaValue {
        if (obj == null) return LuaValue.NIL

        // 创建自定义 Userdata
        return when (obj) {
            is Event, is CommandSender -> objectLuaCache.getOrPut(obj) { createCustomUserdata(obj) }
            else -> CoerceJavaToLua.coerce(obj)
        }
    }

    /**
     * 为指定的 Java 对象创建自定义 LuaUserdata，并设置元表
     *
     * @param obj Java 对象
     * @return 包装后的 LuaUserdata
     */
    private fun createCustomUserdata(obj: Any): LuaUserdata {
        val userdata = LuaUserdata(obj)
        val metatable = metatableCache.computeIfAbsent(obj.javaClass) { clazz ->
            createMetatable(clazz)
        }
        userdata.setmetatable(metatable)
        return userdata
    }

    /**
     * 为 Java 类生成 Lua 用的元表
     * 自动绑定 getter/setter 访问
     *
     * @param clazz Java 类对象
     * @return 构建好的 LuaTable
     */
    private fun createMetatable(clazz: Class<*>): LuaTable {
        val metatable = LuaTable()

        // 获取 getter/setter
        val getters = getGetterMethods(clazz)
        val setters = getSetterMethods(clazz)

        metatable.set(LuaValue.INDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1()
                val key = args.arg(2).tojstring()

                // 在 key 包含下划线时进行转换尝试
                val camelCaseKey = if (key.contains('_')) snakeToCamel(key) else key

                val javaObject = self.checkuserdata(Any::class.java) ?: return NIL

                // 尝试查找 getter
                val getterMethod = getters[camelCaseKey]
                if (getterMethod != null) {
                    return try {
                        createLuaValue(getterMethod.invoke(javaObject))
                    } catch (e: Exception) {
                        PLog.warning("log.warning.getter_failed", camelCaseKey, e.message ?: "Unknown error")
                        NIL
                    }
                }

                // Cancellable 特判
                if (key == "cancelled" && javaObject is Cancellable)
                    return valueOf(javaObject.isCancelled)

                // 尝试查找缓存或实时查找方法
                val methods = getMethodsByName(clazz, camelCaseKey)

                return if (methods.isNotEmpty()) {
                    // 如果找到了方法，返回一个 Lua 函数来调用它
                    object : VarArgFunction() {
                        override fun invoke(callArgs: Varargs): Varargs {
                            // 第一个参数是 self
                            val callSelf = callArgs.arg1()
                            val callJavaObject = callSelf.checkuserdata(Any::class.java) ?: return NIL

                            // 尝试找到匹配的方法 (基于参数数量和类型)
                            for (method in methods) {
                                val paramCount = method.parameterCount
                                if (callArgs.narg() - 1 == paramCount) {  // -1 是因为第一个参数是 self
                                    val javaArgs = Array(paramCount) { i ->
                                        val luaArg = callArgs.arg(i + 2)  // +2 是因为 arg 索引从1开始, 且跳过 self
                                        // 尝试转换参数类型
                                        coerceLuaToJava(luaArg, method.parameterTypes[i])
                                            ?: throw IllegalArgumentException("Cannot coerce Lua value ${luaArg.typename()} to Java type ${method.parameterTypes[i].simpleName}")
                                    }
                                    val result = method.invoke(callJavaObject, *javaArgs)
                                    return createLuaValue(result)
                                }
                            }
                            // 如果没有找到完全匹配的重载方法
                            PLog.warning("log.warning.method_not_match", camelCaseKey)
                            return NIL
                        }
                    }
                } else {
                    // 如果 getter 和方法都没有找到
                    PLog.warning("log.warning.method_not_found", camelCaseKey)
                    NIL
                }
            }
        })

        metatable.set(LuaValue.NEWINDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1()
                val key = args.arg(2).tojstring()
                val value = args.arg(3)

                val camelCaseKey = if (key.contains('_')) snakeToCamel(key) else key

                val javaObject = self.checkuserdata(Any::class.java) ?: return NIL

                val method = setters[camelCaseKey]

                // 特殊处理 Cancellable 的 cancelled 字段
                if (key == "cancelled" && javaObject is Cancellable) {
                    javaObject.isCancelled = value.optboolean(false)
                    return NIL
                }

                return try {
                    if (method != null) {
                        val paramType = method.parameterTypes[0]
                        val javaValue = coerceLuaToJava(value, paramType)
                        if (javaValue != null) {
                            method.invoke(javaObject, javaValue)
                        }
                    }
                    NIL
                } catch (e: Exception) {
                    PLog.warning("log.warning.setter_failed", camelCaseKey, e.message ?: "Unknown error")
                    NIL
                }
            }
        })

        metatable.set(LuaValue.TOSTRING, object : OneArgFunction() {
            override fun call(self: LuaValue): LuaValue {
                val javaObject = self.checkuserdata(Any::class.java)
                return valueOf("JavaObject<${javaObject?.javaClass?.simpleName ?: "null"}>@${javaObject?.hashCode() ?: 0}")
            }
        })

        return metatable
    }

    /**
     * 提取指定类中符合 getter 规范的方法（无参数、以 get/is 开头）
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
     * 提取指定类中符合 setter 规范的方法（一个参数、以 set 开头）
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
     * 将 LuaValue 根据目标 Java 类型进行强制类型转换
     * 支持基本类型和 userdata
     *
     * @param value LuaValue
     * @param targetType Java 目标类型
     * @return 转换后的 Java 对象，或 null（不支持的类型）
     */
    private fun coerceLuaToJava(value: LuaValue, targetType: Class<*>): Any? = when {
        value.isboolean() && (targetType == Boolean::class.java || targetType == Boolean::class.javaPrimitiveType) -> value.toboolean()
        value.isint() && (targetType == Int::class.java || targetType == Int::class.javaPrimitiveType) -> value.toint()
        value.islong() && (targetType == Long::class.java || targetType == Long::class.javaPrimitiveType) -> value.tolong()
        value.isnumber() && (targetType == Float::class.java || targetType == Float::class.javaPrimitiveType) -> value.tofloat()
        value.isnumber() && (targetType == Double::class.java || targetType == Double::class.javaPrimitiveType) -> value.todouble()
        value.isstring() && targetType == String::class.java -> value.tojstring()
        value.isuserdata() && targetType.isAssignableFrom(value.touserdata()::class.java) -> value.touserdata()
        else -> null
    }

    /**
     * 将蛇形命名法转换为驼峰命名法
     */
    private fun snakeToCamel(input: String): String {
        return snakeToCamelCache.computeIfAbsent(input) { key ->
            val parts = key.split('_')
            if (parts.size <= 1) {
                return@computeIfAbsent key
            }
            val builder = StringBuilder(parts[0])
            for (i in 1 until parts.size) {
                val part = parts[i]
                if (part.isNotEmpty()) {
                    builder.append(part.substring(0, 1).uppercase())
                    if (part.length > 1) {
                        builder.append(part.substring(1))
                    }
                }
            }
            builder.toString()
        }
    }
}