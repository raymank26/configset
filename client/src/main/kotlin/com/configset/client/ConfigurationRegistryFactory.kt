package com.configset.client

import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.repository.ConfigurationRepository
import com.configset.client.repository.grpc.GrpcClientFactory
import com.configset.client.repository.grpc.GrpcConfigurationRepository
import com.configset.client.repository.local.FileTransportRepository
import com.configset.common.client.grpc.DeadlineInterceptor
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

object ConfigurationRegistryFactory {

    fun getConfiguration(transport: ConfigurationSource): ConfigurationRegistry<Configuration> {
        val repository = when (transport) {
            is ConfigurationSource.Grpc -> createGrpcConfiguration(transport)
            is ConfigurationSource.File -> createFileRepository(transport)
        }
        return getConfiguration(repository)
    }

    fun getUpdatableLocalConfiguration(localClasspath: ConfigurationSource.File): ConfigurationRegistry<UpdatableConfiguration> {
        val repository = createFileRepository(localClasspath)
        repository.start()

        return getConfiguration(repository)
    }

    internal fun <T : Configuration> getConfiguration(repository: ConfigurationRepository): ConfigurationRegistry<T> {
        val registry = ConfigurationRegistry<T>(repository)
        registry.start()
        return registry
    }

    private fun createGrpcConfiguration(transport: ConfigurationSource.Grpc): ConfigurationRepository {
        return GrpcConfigurationRepository(
            applicationHostname = transport.hostName,
            defaultApplicationName = transport.defaultApplicationName,
            grpcClientFactory = object : GrpcClientFactory {
                override fun createAsyncClient(): ConfigurationServiceGrpc.ConfigurationServiceStub {
                    return prepareGrpcStub(transport)
                }
            },
            reconnectionTimeoutMs = 5000
        )
    }

    private fun prepareGrpcStub(transport: ConfigurationSource.Grpc): ConfigurationServiceGrpc.ConfigurationServiceStub {
        val channel = ManagedChannelBuilder.forAddress(transport.backendHost, transport.backendPort)
            .usePlaintext()
            .keepAliveTime(5000, TimeUnit.MILLISECONDS)
            .build()
        return ConfigurationServiceGrpc.newStub(channel).withInterceptors(DeadlineInterceptor(transport.deadlineMs))
    }

    private fun createFileRepository(fileTransport: ConfigurationSource.File): ConfigurationRepository {
        return FileTransportRepository(fileTransport)
    }
}
