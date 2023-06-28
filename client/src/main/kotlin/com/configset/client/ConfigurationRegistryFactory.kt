package com.configset.client

import com.configset.client.proto.ConfigurationServiceGrpc
import com.configset.client.repository.ConfigurationRepository
import com.configset.client.repository.grpc.GrpcClientFactory
import com.configset.client.repository.grpc.GrpcConfigurationRepository
import com.configset.client.repository.local.FileTransportRepository
import com.configset.common.client.extension.createLoggerStatic
import com.configset.common.client.grpc.DeadlineInterceptor
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

private val logger = createLoggerStatic<ConfigurationRepository>()

object ConfigurationRegistryFactory {

    fun getConfiguration(
        uriKey: String = "CONFIG_URI",
        env: Map<String, String> = System.getenv(),
    ): ConfigurationRegistry<Configuration> {

        val transport = ConfigurationSourceUriParser.parse(env[uriKey]!!)
        return getConfiguration(transport)
    }

    fun getConfiguration(source: ConfigurationSource): ConfigurationRegistry<Configuration> {
        logger.info("Creating configuration from source = $source")
        val repository = when (source) {
            is ConfigurationSource.Grpc -> createGrpcConfiguration(source)
            is ConfigurationSource.File -> createFileRepository(source)
        }
        return getConfiguration(repository)
    }

    fun getUpdatableLocalConfiguration(localClasspath: ConfigurationSource.File): ConfigurationRegistry<UpdatableConfiguration> {
        logger.info("Creating updatable configuration from source = $localClasspath")
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
