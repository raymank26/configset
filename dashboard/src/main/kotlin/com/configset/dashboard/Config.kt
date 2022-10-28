package com.configset.dashboard

class Config(properties: Map<String, String>) {

    val hostname = properties["config_server.hostname"] ?: "localhost"
    val port = properties["config_server.port"]?.toInt() ?: 8988
    val dashboardPort = properties["dashboard.port"]?.toInt() ?: 8188
    val templatesFilePath = properties["templates.file.path"]
    val jsFilePath = properties["js.file.path"]

    val authenticationConfig = AuthenticationConfig(properties)
}

class AuthenticationConfig(properties: Map<String, String>) {
    val realmUri: String = requireProp(properties, "auth.realm_uri")
    val authUri = requireProp(properties, "auth.auth_uri")
    val requestTokenUri = requireProp(properties, "auth.request_token_uri")
    val authRedirectUri = requireProp(properties, "auth.redirect_uri")
    val authClientId = requireProp(properties, "auth.client_id")
    val authSecretKey = requireProp(properties, "auth.secret_key")
}

private fun requireProp(properties: Map<String, String>, key: String): String {
    return requireNotNull(properties[key]) { "'$key' is not found" }
}
