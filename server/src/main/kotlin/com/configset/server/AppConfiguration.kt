package com.configset.server

class AppConfiguration {

    private val env = System.getenv()

    fun validate() {
        if (getDaoType() == DaoType.POSTGRES) {
            getJdbcUrl()
        }
    }

    fun getDaoType(): DaoType {
        return when (readFromEnv("db_type", "memory")) {
            "postgres" -> DaoType.POSTGRES
            else -> DaoType.IN_MEMORY
        }
    }

    fun getJdbcUrl(): String {
        return readFromEnv("jdbc_url")
    }

    fun getUpdateDelayMs(): Long {
        return readFromEnv("update_delay_ms", "4000").toLong()
    }

    fun grpcPort(): Int {
        return 8080
    }

    private fun readFromEnv(key: String, default: String): String {
        return env[key] ?: default
    }

    private fun readFromEnv(key: String): String {
        return env[key] ?: throw ConfigKeyRequired(key)
    }
}

class ConfigKeyRequired(val configKey: String) : RuntimeException() {
}

enum class DaoType {
    IN_MEMORY,
    POSTGRES
    ;
}