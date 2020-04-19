package com.letsconfig.client

import com.letsconfig.client.repository.GrpcConfigurationRepository

object ConfigurationFactory {

    fun getConfiguration(applicationName: String, transport: ConfigurationTransport): Configuration {
        return when (transport) {
            is ConfigurationTransport.RemoteGrpc -> createGrpcConfiguration(applicationName, transport)
            is ConfigurationTransport.LocalClasspath -> TODO()
        }
    }

    private fun createGrpcConfiguration(applicationName: String, transport: ConfigurationTransport.RemoteGrpc): Configuration {
        val repository = GrpcConfigurationRepository(transport.applicaitonHostName, transport.backendHost, transport.backendPort)
        return ConfigurationRegistry(repository).getConfiguration(applicationName)
    }
}