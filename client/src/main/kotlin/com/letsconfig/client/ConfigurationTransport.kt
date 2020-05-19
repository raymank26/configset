package com.letsconfig.client

import com.letsconfig.client.metrics.LibraryMetrics
import com.letsconfig.client.metrics.NoopMetrics

sealed class ConfigurationTransport {

    abstract val libraryMetrics: LibraryMetrics

    data class RemoteGrpc(
            val hostName: String,
            val defaultApplicationName: String,
            val backendHost: String,
            val backendPort: Int,
            override val libraryMetrics: LibraryMetrics = NoopMetrics

    ) : ConfigurationTransport()

    class LocalClasspath(
            val pathToProperties: String,
            override val libraryMetrics: LibraryMetrics = NoopMetrics
    ) : ConfigurationTransport()
}
