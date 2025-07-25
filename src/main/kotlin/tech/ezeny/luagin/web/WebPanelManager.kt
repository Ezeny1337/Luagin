package tech.ezeny.luagin.web

import org.bukkit.configuration.file.YamlConfiguration
import tech.ezeny.luagin.config.YamlManager
import tech.ezeny.luagin.utils.FileUtils
import tech.ezeny.luagin.utils.PLog
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
import tech.ezeny.luagin.performance.PerformanceMonitor
import io.ktor.http.Cookie
import io.ktor.http.HttpStatusCode
import io.ktor.http.auth.HttpAuthHeader

class WebPanelManager(private val yamlManager: YamlManager, private val performanceMonitor: PerformanceMonitor) {
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
                password = authSection.getString("password", "admin") ?: "admin"
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