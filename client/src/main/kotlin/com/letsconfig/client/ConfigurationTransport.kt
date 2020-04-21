package com.letsconfig.client

sealed class ConfigurationTransport {
    data class RemoteGrpc(
            val applicationHostName: String,
            val defaultApplicationName: String,
            val backendHost: String,
            val backendPort: Int
    ) : ConfigurationTransport()

    class LocalClasspath(val pathToProperties: String) : ConfigurationTransport()
}
