package com.configset.client

import java.net.URI

sealed class ConfigurationSource {

    data class Grpc(
        val hostName: String,
        val defaultApplicationName: String,
        val backendHost: String,
        val backendPort: Int,
        val deadlineMs: Long,
    ) : ConfigurationSource()

    data class File(
        val path: URI,
        val location: FileLocation,
        val format: FileFormat,
    ) : ConfigurationSource()
}
