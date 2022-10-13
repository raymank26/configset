package com.configset.server

class AppConfiguration(private val properties: Map<String, String>) {

    fun validate() {
        if (getDaoType() == DaoType.POSTGRES) {
            getJdbcUrl()
        }
    }

    fun getDaoType(): DaoType {
        return when (val type = readProperty("db_type", "memory")) {
            "postgres" -> DaoType.POSTGRES
            "memory" -> DaoType.IN_MEMORY
            else -> error("unknown type $type")
        }
    }

    fun getAuthenticatorConfig(): AuthenticatorConfig {
        return when (val type = readProperty("authenticator_type")) {
            "stub" -> {
                val adminAccessToken = readProperty("auth_stub.admin_access_token")
                StubAuthenticatorConfig(adminAccessToken)
            }
            "oauth" -> AuthConfiguration(
                baseUrl = readProperty("oauth_provider_url"),
                timeoutMs = readProperty("oauth_timeout_ms", "2000").toLong(),
                clientId = readProperty("client_id")
            )
            else -> error("unknown type $type")
        }
    }

    fun getJdbcUrl(): String {
        return readProperty("jdbc_url")
    }

    fun getUpdateDelayMs(): Long {
        return readProperty("update_delay_ms", "4000").toLong()
    }

    fun grpcPort(): Int {
        return readProperty("grpc_port", "8080").toInt()
    }

    private fun readProperty(key: String, default: String): String {
        return properties[key] ?: default
    }

    private fun readProperty(key: String): String {
        return properties[key] ?: throw ConfigKeyRequired(key)
    }
}

class ConfigKeyRequired(val configKey: String) : RuntimeException("Configuration key not found '$configKey'")

enum class DaoType {
    IN_MEMORY,
    POSTGRES,
    ;
}


sealed interface AuthenticatorConfig

data class StubAuthenticatorConfig(val adminAccessToken: String) : AuthenticatorConfig
data class AuthConfiguration(
    val baseUrl: String,
    val timeoutMs: Long,
    val clientId: String,
) : AuthenticatorConfig
