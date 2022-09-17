package com.configset.dashboard


class Config(properties: Map<String, String>) {

    val hostname = properties["config_server.hostname"] ?: "localhost"
    val port = properties["config_server.port"]?.toInt() ?: 8988
    val dashboardPort = properties["dashboard.port"]?.toInt() ?: 8188
    val templatesFilePath = properties["templates.file.path"]

    // client
    val keycloackUrl = requireProp(properties, "client.keycloack_url")
    val keycloackRealm = requireProp(properties, "client.keycloack_realm")
    val keycloackClientId = requireProp(properties, "client.keycloack_clientId")
}

private fun requireProp(properties: Map<String, String>, key: String): String {
    return requireNotNull(properties[key]) { "'$key' is not found" }
}
