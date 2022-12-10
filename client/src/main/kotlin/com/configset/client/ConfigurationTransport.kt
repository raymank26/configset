package com.configset.client

sealed class ConfigurationTransport {

    data class RemoteGrpc(
        val hostName: String,
        val defaultApplicationName: String,
        val backendHost: String,
        val backendPort: Int,
        val deadlineMs: Long
    ) : ConfigurationTransport()

    class LocalClasspath(
        val pathToProperties: String,
    ) : ConfigurationTransport()
}
