package com.letsconfig.client

sealed class ConfigurationTransport {
    data class RemoteGrpc(val applicaitonHostName: String, val backendHost: String, val backendPort: Int) : ConfigurationTransport()
    class LocalClasspath(path: String) : ConfigurationTransport()
}
