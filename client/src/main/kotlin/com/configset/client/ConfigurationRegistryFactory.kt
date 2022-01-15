package com.configset.client

import com.configset.client.repository.ConfigurationRepository
import com.configset.client.repository.grpc.GrpcConfigurationRepository
import com.configset.client.repository.local.LocalConfigurationRepository
import com.configset.sdk.proto.ConfigurationServiceGrpc
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

object ConfigurationRegistryFactory {

    fun getConfiguration(transport: ConfigurationTransport): ConfigurationRegistry {
        val repository = when (transport) {
            is ConfigurationTransport.RemoteGrpc -> createGrpcConfiguration(transport)
            is ConfigurationTransport.LocalClasspath -> createLocalClasspath(transport)
        }
        return getConfiguration(repository)
    }

    internal fun getConfiguration(repository: ConfigurationRepository): ConfigurationRegistry {
        val registry = ConfigurationRegistry(repository)
        registry.start()
        return registry
    }

    private fun createGrpcConfiguration(transport: ConfigurationTransport.RemoteGrpc): ConfigurationRepository {
        return GrpcConfigurationRepository(transport.hostName, transport.defaultApplicationName) {
            prepareGrpcStub(transport)
        }
    }

    private fun prepareGrpcStub(transport: ConfigurationTransport.RemoteGrpc): ConfigurationServiceGrpc.ConfigurationServiceStub {
        val channel = ManagedChannelBuilder.forAddress(transport.backendHost, transport.backendPort)
            .usePlaintext()
            .keepAliveTime(5000, TimeUnit.MILLISECONDS)
            .build()
        return ConfigurationServiceGrpc.newStub(channel)
    }

    private fun createLocalClasspath(transport: ConfigurationTransport.LocalClasspath): ConfigurationRepository {
        return LocalConfigurationRepository {
            ConfigurationRegistryFactory::class.java.getResourceAsStream(transport.pathToProperties).reader()
        }
    }
}