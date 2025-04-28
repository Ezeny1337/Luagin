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
import tech.ezeny.luaKit.utils.PLog
import java.lang.reflect.Method
import kotlin.jvm.java

object LuaValueFactory {
    // 缓存类和属性方法的映射
    private val methodCache: MutableMap<Class<*>, Map<String, Method>> = mutableMapOf()

    /**
     * 将 Java 对象转换为 Lua 值
     *
     * @param obj 需要转换的 Java 对象
     * @return 转换后的 Lua 值
     */
    fun createLuaValue(obj: Any?): LuaValue {
        if (obj == null) {
            return LuaValue.NIL
        }
        // 为 Bukkit Event 创建自定义 Userdata
        return if (obj is Event) {
            createCustomUserdata(obj)
        } else {
            CoerceJavaToLua.coerce(obj)
        }
    }

    /**
     * 为 Java 对象创建一个自定义的 LuaUserdata，包含索引和新索引元方法
     * 支持 getter 和 setter 的调用
     *
     * @param javaObject Java 对象
     * @return 创建的 LuaUserdata
     */
    private fun createCustomUserdata(javaObject: Any): LuaUserdata {
        val userdata = LuaUserdata(javaObject)
        val metatable = LuaTable()

        // 设置 __index 元方法
        metatable.set(LuaValue.INDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(2).isstring()) {
                    return NIL
                }

                val self = args.arg1()
                val key = args.arg(2).tojstring()
                val currentJavaObject = self.checkuserdata(Any::class.java) ?: return NIL

                // 特殊处理 Cancellable 接口的 cancelled 属性
                if (key == "cancelled" && currentJavaObject is Cancellable)
                    return valueOf(currentJavaObject.isCancelled)

                // 尝试从缓存中获取 getter 方法
                val methods = getMethods(currentJavaObject::class.java)
                val getterName = "get" + key.replaceFirstChar { it.titlecase() }
                val isGetterName = "is" + key.replaceFirstChar { it.titlecase() }

                val getterMethod = methods[getterName] ?: methods[isGetterName]

                try {
                    if (getterMethod != null) {
                        // 找到 getter，调用并返回结果
                        val result = getterMethod.invoke(currentJavaObject)
                        return createLuaValue(result)
                    }
                } catch (e: Exception) {
                    PLog.warning("log.warning.getter_error", getterName, isGetterName, e.message ?: "Unknown error")
                }

                return NIL
            }
        })

        // 设置 __newindex 元方法
        metatable.set(LuaValue.NEWINDEX, object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 3 || !args.arg(2).isstring()) {
                    return NIL
                }

                val self = args.arg1()
                val key = args.arg(2).tojstring()
                val value = args.arg(3)
                val currentJavaObject = self.checkuserdata(Any::class.java) ?: return NIL

                // 特殊处理 Cancellable 接口的 cancelled 属性
                if (key == "cancelled" && currentJavaObject is Cancellable) {
                    if (value.isboolean()) {
                        currentJavaObject.isCancelled = value.toboolean()
                        return NIL
                    } else if (value.isstring()) {
                        // 尝试将字符串转换为布尔值
                        val boolStr = value.tojstring().trim()
                        if (boolStr.equals("true", ignoreCase = true)) {
                            currentJavaObject.isCancelled = true
                            return NIL
                        } else if (boolStr.equals("false", ignoreCase = true)) {
                            currentJavaObject.isCancelled = false
                            return NIL
                        }
                    }
                }

                // 尝试从缓存中获取 setter 方法
                val methods = getMethods(currentJavaObject::class.java)
                val setterName = "set" + key.replaceFirstChar { it.titlecase() }

                try {
                    val setterMethod = methods[setterName]
                    if (setterMethod != null) {
                        // 找到 setter，尝试调用
                        val paramType = setterMethod.parameterTypes[0]
                        // 根据参数类型转换 Lua 值
                        val javaValue = when {
                            value.isboolean() && (paramType == Boolean::class.java || paramType == Boolean::class.javaPrimitiveType) -> value.toboolean()
                            value.isint() && (paramType == Int::class.java || paramType == Int::class.javaPrimitiveType) -> value.toint()
                            value.islong() && (paramType == Long::class.java || paramType == Long::class.javaPrimitiveType) -> value.tolong()
                            value.isstring() && paramType == String::class.java -> value.tojstring()
                            value.isuserdata() -> value.touserdata()
                            else -> null
                        }

                        if (javaValue != null) {
                            setterMethod.invoke(currentJavaObject, javaValue)
                            return NIL
                        }
                    }
                } catch (e: Exception) {
                    PLog.warning("log.warning.set_property_error", key, e.message ?: "Unknown error")
                }

                PLog.warning("log.warning.set_property_error_not_found", key)
                return NIL
            }
        })

        // 设置 __tostring 元方法
        metatable.set(LuaValue.TOSTRING, object : OneArgFunction() {
            override fun call(self: LuaValue): LuaValue {
                val currentJavaObject = self.checkuserdata(Any::class.java)
                return valueOf("JavaObject<${currentJavaObject?.javaClass?.simpleName ?: "null"}>@${currentJavaObject?.hashCode() ?: 0}")
            }
        })

        userdata.setmetatable(metatable)
        return userdata
    }

    /**
     * 从缓存中获取类的所有方法，若缓存中没有，则通过反射获取并缓存
     * 使用方法名作为缓存的 key，以便快速查找
     *
     * @param clazz 需要获取方法的类
     * @return 方法名称到方法对象的映射
     */
    private fun getMethods(clazz: Class<*>): Map<String, Method> {
        return methodCache.getOrPut(clazz) {
            clazz.methods.associateBy { it.name }
        }
    }
}