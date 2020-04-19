package com.letsconfig.client

import com.letsconfig.client.repository.grpc.GrpcConfigurationRepository

object ConfigurationFactory {

    fun getConfiguration(transport: ConfigurationTransport): ConfigurationRegistry {
        return when (transport) {
            is ConfigurationTransport.RemoteGrpc -> createGrpcConfiguration(transport)
            is ConfigurationTransport.LocalClasspath -> TODO()
        }
    }

    private fun createGrpcConfiguration(transport: ConfigurationTransport.RemoteGrpc): ConfigurationRegistry {
        val repository = GrpcConfigurationRepository(transport.applicaitonHostName, transport.backendHost, transport.backendPort)
        val registry = ConfigurationRegistry(repository)
        registry.start()
        return registry
    }
}