package tech.ezeny.luagin.web

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.fasterxml.jackson.databind.SerializationFeature
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.http.content.staticResources
import io.ktor.server.netty.Netty
import io.ktor.server.netty.NettyApplicationEngine
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.cors.routing.CORS
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader
import io.ktor.server.routing.delete
import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.performance.PerformanceMonitor
import tech.ezeny.luagin.permissions.PermissionManager
import tech.ezeny.luagin.utils.FileUtils
import tech.ezeny.luagin.utils.PLog

class WebPanelManager(
    private val yamlManager: YamlManager,
    private val performanceMonitor: PerformanceMonitor,
    private val permissionManager: PermissionManager
) {
    val config: WebPanelConfig = getWebPanelConfig()
    private var server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>? = null

    init {
        if (config.enabled) {
            try {
                start(9527)
                PLog.info("log.info.webpanel_started", "9527")
            } catch (e: Exception) {
                PLog.warning("log.warning.webpanel_start_failed", e.message ?: "Unknown error")
            }
        } else {
            PLog.info("log.info.webpanel_disabled")
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun start(port: Int) {
        if (server != null) return
        val jwtSecret = config.auth.jwtSecret.ifBlank { "LuaginDefaultSecret" }
        server = embeddedServer(Netty, port = port) {
            install(CORS) {
                anyHost()
                allowHeader("Authorization")
                allowHeader("Content-Type")
            }
            install(ContentNegotiation) {
                jackson {
                    configure(SerializationFeature.INDENT_OUTPUT, true)
                }
            }
            install(Authentication) {
                jwt("auth-jwt") {
                    verifier(
                        JWT.require(Algorithm.HMAC256(jwtSecret))
                            .withIssuer("luagin-webpanel")
                            .build()
                    )
                    validate { credential ->
                        if (credential.payload.getClaim("username").asString() == config.auth.username) JWTPrincipal(
                            credential.payload
                        ) else null
                    }
                    // 从 cookie 读取 token
                    authHeader {
                        val authHeader = it.request.headers["Authorization"]
                        if (authHeader != null && authHeader.startsWith("Bearer ")) {
                            return@authHeader HttpAuthHeader.Single("Bearer", authHeader.removePrefix("Bearer ").trim())
                        }
                        val cookie = it.request.cookies["luagin_token"]
                        if (cookie != null) {
                            return@authHeader HttpAuthHeader.Single("Bearer", cookie)
                        }
                        null
                    }
                }
            }
            routing {
                post("/api/login") {
                    val params = call.receive<Map<String, String>>()
                    val username = params["username"] ?: ""
                    val password = params["password"] ?: ""
                    if (username == config.auth.username && password == config.auth.password) {
                        val token = JWT.create()
                            .withIssuer("luagin-webpanel")
                            .withClaim("username", username)
                            .sign(Algorithm.HMAC256(jwtSecret))
                        call.response.cookies.append(
                            Cookie(
                                name = "luagin_token",
                                value = token,
                                httpOnly = true,
                                maxAge = 1800, // 30分钟
                                path = "/"
                            )
                        )
                        call.respond(mapOf("token" to token))
                    } else {
                        call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                    }
                }
                authenticate("auth-jwt") {
                    get("/api/perf/all") {
                        call.respond(performanceMonitor.getAllPerformanceData())
                    }
                    get("/api/perf/server") {
                        call.respond(performanceMonitor.getPerformanceData("server") ?: emptyMap())
                    }
                    get("/api/perf/java") {
                        call.respond(performanceMonitor.getPerformanceData("java") ?: emptyMap())
                    }
                    get("/api/perf/system") {
                        call.respond(performanceMonitor.getPerformanceData("system") ?: emptyMap())
                    }
                    get("/api/perf/tps") {
                        call.respond(performanceMonitor.getServerTPS().toList())
                    }

                    // 配置管理API
                    get("/api/config/all") {
                        val configs = mutableMapOf<String, Any>()

                        // 获取WebPanel配置
                        val webpanelConfig = yamlManager.getConfig("configs/webpanel.yml")
                        configs["webpanel"] = if (webpanelConfig != null) {
                            mapOf(
                                "enabled" to webpanelConfig.getBoolean("enabled", true),
                                "auth" to mapOf(
                                    "enabled" to webpanelConfig.getBoolean("auth.enabled", true),
                                    "username" to webpanelConfig.getString("auth.username", "admin"),
                                    "password" to webpanelConfig.getString("auth.password", "admin"),
                                    "jwtSecret" to webpanelConfig.getString("auth.jwtSecret", "LuaginDefaultSecret")
                                )
                            )
                        } else {
                            mapOf(
                                "enabled" to true,
                                "auth" to mapOf(
                                    "enabled" to true,
                                    "username" to "admin",
                                    "password" to "admin",
                                    "jwtSecret" to "LuaginDefaultSecret"
                                )
                            )
                        }

                        // 获取MySQL配置
                        val mysqlConfig = yamlManager.getConfig("configs/mysql.yml")
                        configs["mysql"] = if (mysqlConfig != null) {
                            mapOf(
                                "enable" to mysqlConfig.getBoolean("enable", false),
                                "host" to mysqlConfig.getString("host", "localhost"),
                                "port" to mysqlConfig.getInt("port", 3306),
                                "database" to mysqlConfig.getString("database", "luagin"),
                                "username" to mysqlConfig.getString("username", "root"),
                                "password" to mysqlConfig.getString("password", ""),
                                "pool-size" to mysqlConfig.getInt("pool-size", 10)
                            )
                        } else {
                            mapOf(
                                "enable" to false,
                                "host" to "localhost",
                                "port" to 3306,
                                "database" to "luagin",
                                "username" to "root",
                                "password" to "",
                                "pool-size" to 10
                            )
                        }

                        // 获取权限配置
                        val permissionsConfig = yamlManager.getConfig("configs/permissions.yml")
                        configs["permissions"] = if (permissionsConfig != null) {
                            val groupsSection = permissionsConfig.getConfigurationSection("groups")
                            val playersSection = permissionsConfig.getConfigurationSection("players")

                            val groupsMap = groupsSection?.getValues(false)?.filterValues { it != null }
                                ?.mapValues { it.value as Any } ?: emptyMap<String, Any>()
                            val playersMap = playersSection?.getValues(false)?.filterValues { it != null }
                                ?.mapValues { it.value as Any } ?: emptyMap<String, Any>()

                            mapOf(
                                "groups" to groupsMap,
                                "players" to playersMap
                            )
                        } else {
                            mapOf(
                                "groups" to emptyMap(),
                                "players" to emptyMap()
                            )
                        }

                        call.respond(configs)
                    }

                    post("/api/config/{configName}") {
                        val configName = call.parameters["configName"] ?: return@post call.respondText(
                            "Config name required",
                            status = HttpStatusCode.BadRequest
                        )
                        val configData = call.receive<Map<String, Any>>()

                        when (configName) {
                            "webpanel" -> {
                                val configMap = mutableMapOf<String, Any>()
                                configMap["enabled"] = configData["enabled"] ?: true

                                val auth = configData["auth"] as? Map<String, Any>
                                if (auth != null) {
                                    configMap["auth"] = mapOf(
                                        "enabled" to (auth["enabled"] ?: true),
                                        "username" to (auth["username"] ?: "admin"),
                                        "password" to (auth["password"] ?: "admin"),
                                        "jwtSecret" to (auth["jwtSecret"] ?: "LuaginDefaultSecret")
                                    )
                                }

                                if (yamlManager.createOrUpdateConfig("configs/webpanel.yml", configMap)) {
                                    call.respond(mapOf("success" to true))
                                } else {
                                    call.respondText(
                                        "Failed to save webpanel config",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                }
                            }

                            "mysql" -> {
                                val configMap = mapOf(
                                    "enable" to (configData["enable"] ?: false),
                                    "host" to (configData["host"] ?: "localhost"),
                                    "port" to (configData["port"] ?: 3306),
                                    "database" to (configData["database"] ?: "luagin"),
                                    "username" to (configData["username"] ?: "root"),
                                    "password" to (configData["password"] ?: ""),
                                    "pool-size" to (configData["pool-size"] ?: 10)
                                )

                                if (yamlManager.createOrUpdateConfig("configs/mysql.yml", configMap)) {
                                    call.respond(mapOf("success" to true))
                                } else {
                                    call.respondText(
                                        "Failed to save mysql config",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                }
                            }

                            "permissions" -> {
                                val configMap = mutableMapOf<String, Any>()

                                val groups = configData["groups"] as? Map<String, Any>
                                if (groups != null) {
                                    configMap["groups"] = groups
                                }

                                val players = configData["players"] as? Map<String, Any>
                                if (players != null) {
                                    configMap["players"] = players
                                }

                                if (yamlManager.createOrUpdateConfig("configs/permissions.yml", configMap)) {
                                    call.respond(mapOf("success" to true))
                                } else {
                                    call.respondText(
                                        "Failed to save permissions config",
                                        status = HttpStatusCode.InternalServerError
                                    )
                                }
                            }

                            else -> {
                                call.respondText("Unknown config type", status = HttpStatusCode.BadRequest)
                            }
                        }
                    }

                    // 权限管理 API
                    get("/api/permissions/groups") {
                        val config = permissionManager.getConfig()
                        val groups =
                            config?.getConfigurationSection("groups")?.getKeys(false)?.associateWith { groupName ->
                                mapOf(
                                    "weight" to config.getInt("groups.$groupName.weight", 0),
                                    "permissions" to config.getStringList("groups.$groupName.permissions"),
                                    "inherit" to config.getStringList("groups.$groupName.inherit")
                                )
                            } ?: emptyMap<String, Any>()
                        call.respond(groups)
                    }

                    get("/api/permissions/players") {
                        val config = permissionManager.getConfig()
                        val players =
                            config?.getConfigurationSection("players")?.getKeys(false)?.associateWith { playerName ->
                                mapOf(
                                    "groups" to config.getStringList("players.$playerName.groups"),
                                    "permissions" to config.getStringList("players.$playerName.permissions")
                                )
                            } ?: emptyMap<String, Any>()
                        call.respond(players)
                    }

                    post("/api/permissions/group") {
                        val data = call.receive<Map<String, Any>>()
                        val groupName = data["name"] as? String ?: return@post call.respondText(
                            "Group name required",
                            status = HttpStatusCode.BadRequest
                        )
                        val permissions = data["permissions"] as? List<String> ?: emptyList()
                        val weight = data["weight"] as? Int ?: 0
                        val inherit = data["inherit"] as? List<String> ?: emptyList()

                        if (permissionManager.addGroup(groupName, permissions, weight, inherit)) {
                            call.respond(mapOf("success" to true))
                        } else {
                            call.respondText("Failed to add group", status = HttpStatusCode.InternalServerError)
                        }
                    }

                    delete("/api/permissions/group/{groupName}") {
                        val groupName = call.parameters["groupName"] ?: return@delete call.respondText(
                            "Group name required",
                            status = HttpStatusCode.BadRequest
                        )

                        if (permissionManager.removeGroup(groupName)) {
                            call.respond(mapOf("success" to true))
                        } else {
                            call.respondText("Failed to remove group", status = HttpStatusCode.InternalServerError)
                        }
                    }

                    post("/api/permissions/player") {
                        val data = call.receive<Map<String, Any>>()
                        val playerName = data["name"] as? String ?: return@post call.respondText(
                            "Player name required",
                            status = HttpStatusCode.BadRequest
                        )
                        val groups = data["groups"] as? List<String> ?: emptyList()
                        val permissions = data["permissions"] as? List<String> ?: emptyList()

                        if (permissionManager.setPlayerPermissions(playerName, groups, permissions)) {
                            call.respond(mapOf("success" to true))
                        } else {
                            call.respondText("Failed to save player", status = HttpStatusCode.InternalServerError)
                        }
                    }

                    delete("/api/permissions/player/{playerName}") {
                        val playerName = call.parameters["playerName"] ?: return@delete call.respondText(
                            "Player name required",
                            status = HttpStatusCode.BadRequest
                        )

                        if (permissionManager.removePlayer(playerName)) {
                            call.respond(mapOf("success" to true))
                        } else {
                            call.respondText("Failed to remove player", status = HttpStatusCode.InternalServerError)
                        }
                    }
                }
                staticResources("/", "webpanel/dist")
            }
        }.start(wait = false)
    }

    fun getWebPanelConfig(): WebPanelConfig {
        val config = yamlManager.getConfig("configs/webpanel.yml")
        if (config == null) {
            val defaultConfig = WebPanelConfig()
            saveWebPanelConfig(defaultConfig)
            return defaultConfig
        }
        val enabled = config.getBoolean("enabled", true)
        val authSection = config.getConfigurationSection("auth")
        val auth = if (authSection != null) {
            WebPanelAuthConfig(
                enabled = authSection.getBoolean("enabled", true),
                username = authSection.getString("username", "admin") ?: "admin",
                password = authSection.getString("password", "admin") ?: "admin",
                jwtSecret = authSection.getString("jwtSecret", "LuaginDefaultSecret") ?: "LuaginDefaultSecret"
            )
        } else {
            WebPanelAuthConfig()
        }
        return WebPanelConfig(enabled, auth)
    }

    fun saveWebPanelConfig(config: WebPanelConfig): Boolean {
        val file = FileUtils.getFile("configs/webpanel.yml")
        val yaml = YamlConfiguration()
        yaml.set("enabled", config.enabled)
        yaml.createSection("auth")
        yaml.set("auth.enabled", config.auth.enabled)
        yaml.set("auth.username", config.auth.username)
        yaml.set("auth.password", config.auth.password)
        yaml.set("auth.jwtSecret", config.auth.jwtSecret)
        return try {
            yaml.save(file)
            true
        } catch (e: Exception) {
            PLog.warning("config.save_failed", "configs/webpanel.yml", e.message ?: "Unknown error")
            false
        }
    }

    fun stopWebServer() {
        server?.stop(1000, 2000)
        server = null
    }
}