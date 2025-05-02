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
import kotlin.jvm.java

object LuaValueFactory {
    // 缓存 Event 到 LuaUserdata 的映射
    private val eventLuaCache = WeakHashMap<Event, LuaValue>()

    // 缓存 CommandSender 到 LuaUserdata 的映射
    private val commandSenderLuaCache = WeakHashMap<CommandSender, LuaValue>()

    // 缓存 Class 到 Metatable 的映射
    private val metatableCache = mutableMapOf<Class<*>, LuaTable>()

    // 缓存 Class 到 getter/setter 方法
    private val getterCache = mutableMapOf<Class<*>, Map<String, Method>>()
    private val setterCache = mutableMapOf<Class<*>, Map<String, Method>>()

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
            is Event -> eventLuaCache.getOrPut(obj) { createCustomUserdata(obj) }
            is CommandSender -> commandSenderLuaCache.getOrPut(obj) { createCustomUserdata(obj) }
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
        val metatable = metatableCache.getOrPut(obj.javaClass) {
            createMetatable(obj.javaClass)
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

                // 尝试使用驼峰命名法访问方法
                val method = getters[camelCaseKey]

                // Cancellable 特判
                if (key == "cancelled" && javaObject is Cancellable)
                    return valueOf(javaObject.isCancelled)

                return try {
                    if (method != null) {
                        createLuaValue(method.invoke(javaObject))
                    } else {
                        // 如果没有找到 getter, 尝试查找同名方法
                        val methodName = camelCaseKey
                        val methods = clazz.methods.filter { it.name.equals(methodName, ignoreCase = true) }
                        if (methods.isNotEmpty()) {
                            object : VarArgFunction() {
                                override fun invoke(callArgs: Varargs): Varargs {
                                    // 第一个参数是self
                                    val callSelf = callArgs.arg1()
                                    val callJavaObject = callSelf.checkuserdata(Any::class.java) ?: return NIL

                                    // 尝试找到匹配的方法
                                    for (method in methods) {
                                        try {
                                            val paramCount = method.parameterCount
                                            if (callArgs.narg() - 1 == paramCount) {  // -1 是因为第一个参数是self
                                                val javaArgs = Array(paramCount) { i ->
                                                    val luaArg = callArgs.arg(i + 2)  // +2 是因为arg索引从1开始, 且跳过self
                                                    coerceLuaToJava(luaArg, method.parameterTypes[i])
                                                }
                                                val result = method.invoke(callJavaObject, *javaArgs)
                                                return createLuaValue(result)
                                            }
                                        } catch (e: Exception) {
                                            // 继续尝试下一个方法
                                        }
                                    }
                                    return NIL
                                }
                            }
                        } else {
                            NIL
                        }
                    }
                } catch (e: Exception) {
                    PLog.warning("log.warning.getter_failed", camelCaseKey, e.message ?: "Unknown error")
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
        return getterCache.getOrPut(clazz) {
            clazz.methods
                .filter { it.parameterCount == 0 && (it.name.startsWith("get") || it.name.startsWith("is")) }
                .associateBy {
                    it.name.removePrefix("get").removePrefix("is").replaceFirstChar { c -> c.lowercaseChar() }
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
        return setterCache.getOrPut(clazz) {
            clazz.methods
                .filter { it.parameterCount == 1 && it.name.startsWith("set") }
                .associateBy { it.name.removePrefix("set").replaceFirstChar { c -> c.lowercaseChar() } }
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
    private fun coerceLuaToJava(value: LuaValue, targetType: Class<*>): Any? {
        return when {
            value.isboolean() && (targetType == Boolean::class.java || targetType == Boolean::class.javaPrimitiveType) -> value.toboolean()
            value.isint() && (targetType == Int::class.java || targetType == Int::class.javaPrimitiveType) -> value.toint()
            value.islong() && (targetType == Long::class.java || targetType == Long::class.javaPrimitiveType) -> value.tolong()
            value.isnumber() && (targetType == Float::class.java || targetType == Float::class.javaPrimitiveType) -> value.tofloat()
            value.isnumber() && (targetType == Double::class.java || targetType == Double::class.javaPrimitiveType) -> value.todouble()
            value.isstring() && targetType == String::class.java -> value.tojstring()
            value.isuserdata() && targetType.isAssignableFrom(value.touserdata()::class.java) -> value.touserdata()
            else -> null
        }
    }

    /**
     * 将蛇形命名法转换为驼峰命名法
     */
    private fun snakeToCamel(snake: String): String {
        return snake.split('_').mapIndexed { index, part ->
            if (index == 0) part
            else part.replaceFirstChar { it.uppercaseChar() }
        }.joinToString("")
    }
}