package tech.ezeny.luaKit.lua

import org.bukkit.event.Cancellable
import org.bukkit.event.Event
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.VarArgFunction
import org.luaj.vm2.lib.jse.CoerceJavaToLua
import sun.reflect.misc.MethodUtil.getMethods
import tech.ezeny.luaKit.utils.PLog
import java.lang.reflect.Method
import java.util.WeakHashMap
import kotlin.jvm.java

object LuaValueFactory {
    // 缓存 Event 到 LuaUserdata 的映射
    private val eventLuaCache = WeakHashMap<Event, LuaValue>()

    // 缓存 Class 到 Metatable 的映射
    private val metatableCache = mutableMapOf<Class<*>, LuaTable>()

    // 缓存 Class 到 getter/setter 方法
    private val getterCache = mutableMapOf<Class<*>, Map<String, Method>>()
    private val setterCache = mutableMapOf<Class<*>, Map<String, Method>>()

    /**
     * 将 Java 对象转换为 LuaValue
     * 目前仅 Bukkit Event 类型
     *
     * @param obj 需要转换的 Java 对象
     * @return 转换后的 LuaValue
     */
    fun createLuaValue(obj: Any?): LuaValue {
        if (obj == null) return LuaValue.NIL

        // 为 Bukkit Event 创建自定义 Userdata
        return when (obj) {
            is Event -> eventLuaCache.getOrPut(obj) { createCustomUserdata(obj) }
            else -> CoerceJavaToLua.coerce(obj)
        }
    }

    /**
     * 为指定的 Event 创建自定义 LuaUserdata，并设置元表
     *
     * @param event Bukkit 事件对象
     * @return 包装后的 LuaUserdata
     */
    private fun createCustomUserdata(event: Event): LuaUserdata {
        val userdata = LuaUserdata(event)
        val metatable = metatableCache.getOrPut(event.javaClass) {
            createMetatable(event.javaClass)
        }
        userdata.setmetatable(metatable)
        return userdata
    }

    /**
     * 为 Java 类生成 Lua 用的元表
     * 自动绑定 getter/setter 访问，支持 cancelled 特判
     *
     * @param clazz Java 类对象
     * @return 构建好的 LuaTable
     */
    private fun createMetatable(clazz: Class<*>): LuaTable {
        val metatable = LuaTable()

        // 缓存 getter/setter
        val getters = getGetterMethods(clazz)
        val setters = getSetterMethods(clazz)

        metatable.set(LuaValue.INDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1()
                val key = args.arg(2).tojstring()
                val javaObject = self.checkuserdata(Any::class.java) ?: return NIL

                // Cancellable 特判
                if (key == "cancelled" && javaObject is Cancellable)
                    return valueOf(javaObject.isCancelled)

                val method = getters[key]
                return try {
                    if (method != null) {
                        createLuaValue(method.invoke(javaObject))
                    } else {
                        NIL
                    }
                } catch (e: Exception) {
                    PLog.warning("log.warning.getter_error", key, e.message ?: "Unknown error")
                    NIL
                }
            }
        })

        metatable.set(LuaValue.NEWINDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                val self = args.arg1()
                val key = args.arg(2).tojstring()
                val value = args.arg(3)
                val javaObject = self.checkuserdata(Any::class.java) ?: return NIL

                // 特殊处理 Cancellable 的 cancelled 字段
                if (key == "cancelled" && javaObject is Cancellable) {
                    javaObject.isCancelled = value.optboolean(false)
                    return NIL
                }

                val method = setters[key]
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
                    PLog.warning("log.warning.setter_error", key, e.message ?: "Unknown error")
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
                .associateBy { it.name.removePrefix("get").removePrefix("is").replaceFirstChar { c -> c.lowercaseChar() } }
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
}