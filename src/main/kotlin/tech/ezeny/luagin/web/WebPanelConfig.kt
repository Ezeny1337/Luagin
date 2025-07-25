package tech.ezeny.luagin.web

data class WebPanelConfig(
    val enabled: Boolean = true,
    val auth: WebPanelAuthConfig = WebPanelAuthConfig()
)

data class WebPanelAuthConfig(
    val enabled: Boolean = true,
    val username: String = "admin",
    val password: String = "admin",
    val jwtSecret: String = "LuaginDefaultSecret"
)
