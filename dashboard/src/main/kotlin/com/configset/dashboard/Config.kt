package com.configset.dashboard


class Config(properties: Map<String, String>) {

    val hostname = properties["config_server.hostname"] ?: "localhost"
    val port = properties["config_server.port"]?.toInt() ?: 8988
    val timeout = properties["config_server.timeout"]?.toLong() ?: 2000
    val dashboardPort = properties["dashboard.port"]?.toInt() ?: 8188
    val serveStatic = properties["server.static"]?.toBoolean() ?: false

    // client
    val keycloackUrl = requireProp(properties, "client.keycloackUrl")
    val keycloackRealm = requireProp(properties, "client.keycloackRealm")
    val keycloackClientId = requireProp(properties, "client.keycloackClientId")
}

private fun requireProp(properties: Map<String, String>, key: String): String {
    return requireNotNull(properties[key]) { "'$key' is not found" }
}
