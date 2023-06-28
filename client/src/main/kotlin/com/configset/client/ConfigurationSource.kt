package com.configset.client

sealed class ConfigurationSource {

    data class Grpc(
        val hostName: String,
        val defaultApplicationName: String,
        val backendHost: String,
        val backendPort: Int,
        val deadlineMs: Long,
    ) : ConfigurationSource()

    data class File(
        val path: String,
        val sourceType: FileSourceType,
        val format: FileFormat,
    ) : ConfigurationSource()

}
