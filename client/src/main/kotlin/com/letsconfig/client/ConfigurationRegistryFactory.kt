package com.letsconfig.client

import com.letsconfig.client.repository.ConfigurationRepository
import com.letsconfig.client.repository.grpc.GrpcConfigurationRepository
import com.letsconfig.client.repository.local.LocalConfigurationRepository

object ConfigurationRegistryFactory {

    fun getConfiguration(transport: ConfigurationTransport): ConfigurationRegistry {
        val repository = when (transport) {
            is ConfigurationTransport.RemoteGrpc -> createGrpcConfiguration(transport)
            is ConfigurationTransport.LocalClasspath -> createLocalClasspath(transport)
        }
        val registry = ConfigurationRegistry(repository)
        registry.start()
        return registry
    }

    private fun createGrpcConfiguration(transport: ConfigurationTransport.RemoteGrpc): ConfigurationRepository {
        return GrpcConfigurationRepository(transport.hostName, transport.defaultApplicationName,
                transport.backendHost, transport.backendPort, transport.libraryMetrics)
    }

    private fun createLocalClasspath(transport: ConfigurationTransport.LocalClasspath): ConfigurationRepository {
        return LocalConfigurationRepository {
            ConfigurationRegistryFactory::class.java.getResourceAsStream(transport.pathToProperties).reader()
        }
    }
}