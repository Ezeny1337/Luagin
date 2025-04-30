package tech.ezeny.luagin.lua.api

import org.koin.core.component.KoinComponent
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction
import tech.ezeny.luagin.utils.CommunicationUtils
import tech.ezeny.luagin.utils.PLog

object CommunicationAPI : LuaAPIProvider, KoinComponent {
    private val apiNames = mutableListOf<String>()

    override fun registerAPI(globals: Globals) {
        // 创建通信表
        val scriptCommTable = LuaTable()
        globals.set("comm", scriptCommTable)

        // 暴露函数
        scriptCommTable.set("expose_func", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isfunction()) {
                    return FALSE
                }

                val functionName = args.checkjstring(1)
                val function = args.checkfunction(2)

                val success = CommunicationUtils.exposeFunction(functionName, function)
                return if (success) TRUE else FALSE
            }
        })

        // 取消暴露函数
        scriptCommTable.set("unexpose_func", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 1 || !args.arg(1).isstring()) {
                    return FALSE
                }

                val functionName = args.checkjstring(1)

                val success = CommunicationUtils.unexposeFunction(functionName)
                return if (success) TRUE else FALSE
            }
        })

        // 调用其他脚本函数
        scriptCommTable.set("call_func", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                if (args.narg() < 2 || !args.arg(1).isstring() || !args.arg(2).isstring()) {
                    return NIL
                }

                val scriptName = args.checkjstring(1)
                val functionName = args.checkjstring(2)

                // 收集额外参数
                val functionArgs = if (args.narg() > 2) {
                    val varargs = varargsOf(Array(args.narg() - 2) { i -> args.arg(i + 3) })
                    varargs
                } else {
                    NONE
                }

                return CommunicationUtils.callFunction(scriptName, functionName, functionArgs)
            }
        })

        // 获取所有暴露函数
        scriptCommTable.set("get_exposed_functions", object : VarArgFunction() {
            override fun invoke(args: Varargs): Varargs {
                return CommunicationUtils.getExposedFunctions()
            }
        })

        // 添加到API名称列表
        if (!apiNames.contains("comm")) {
            apiNames.add("comm")
        }

        PLog.info("log.info.comm_api_set")
    }

    override fun getAPINames(): List<String> = apiNames
}