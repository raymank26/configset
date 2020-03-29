package com.letsconfig

import com.letsconfig.network.grpc.GrpcConfigurationServer
import com.letsconfig.network.grpc.GrpcConfigurationService
import com.letsconfig.network.grpc.common.ApplicationRequest
import com.letsconfig.network.grpc.common.ConfigurationServiceGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.After
import org.junit.Before
import org.junit.Test

class GrpcSubscriptionTest {

    private val configurationDao = InMemoryConfigurationDao()

    private val grpcConfServer = GrpcConfigurationServer(
            GrpcConfigurationService(
                    PersistentConfigurationService(configurationDao, PropertiesWatchDispatcher(configurationDao, ConfigurationResolver(), ThreadScheduler(), 1))
            )
    )
    private val blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    private val asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub

    init {
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build()
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)
        asyncClient = ConfigurationServiceGrpc.newStub(channel)
    }

    @Before
    fun before() {
        grpcConfServer.start()
    }

    @After
    fun stop() {
        grpcConfServer.stop()
    }


    @Test
    fun testCreateApplication() {
        val response = blockingClient.createApplication(ApplicationRequest.newBuilder().setApplicationName("Some name").build())
        println(response)
    }
}