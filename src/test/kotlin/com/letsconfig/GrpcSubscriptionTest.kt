package com.letsconfig

import com.letsconfig.network.grpc.GrpcConfigurationServer
import com.letsconfig.network.grpc.GrpcConfigurationService
import com.letsconfig.network.grpc.common.ApplicationCreatedResponse
import com.letsconfig.network.grpc.common.ApplicationRequest
import com.letsconfig.network.grpc.common.ConfigurationServiceGrpc
import com.letsconfig.network.grpc.common.EmptyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class GrpcSubscriptionTest {

    private val configurationDao = InMemoryConfigurationDao(emptyMap())

    private val grpcConfServer = GrpcConfigurationServer(
            GrpcConfigurationService(
                    PersistentConfigurationService(configurationDao, PropertiesWatchDispatcher(
                            configurationDao, ConfigurationResolver(), ThreadScheduler(), 1))
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
        val appName = "Some name"
        val response = blockingClient.createApplication(ApplicationRequest.newBuilder().setApplicationName(appName).build())
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, response.type)

        val result: List<String> = blockingClient.listApplications(EmptyRequest.getDefaultInstance()).applicationList
        Assert.assertEquals(listOf(appName), result)
    }

    @Test
    fun testAddPropertyNoApplication() {
        val result = blockingClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setApplicationName("Some app")
                .setHostName("Some host")
                .setPropertyName("Prop name")
                .setPropertyValue("Some value")
                .setVersion(123L)
                .build())

        Assert.assertEquals(UpdatePropertyResponse.Type.APPLICATION_NOT_FOUND, result.type)
    }
}