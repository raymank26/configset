package com.letsconfig

import com.letsconfig.network.grpc.GrpcConfigurationServer
import com.letsconfig.network.grpc.GrpcConfigurationService
import com.letsconfig.network.grpc.common.ApplicationCreatedResponse
import com.letsconfig.network.grpc.common.ApplicationRequest
import com.letsconfig.network.grpc.common.ApplicationSnapshotResponse
import com.letsconfig.network.grpc.common.ConfigurationServiceGrpc
import com.letsconfig.network.grpc.common.DeletePropertyRequest
import com.letsconfig.network.grpc.common.DeletePropertyResponse
import com.letsconfig.network.grpc.common.PropertyItem
import com.letsconfig.network.grpc.common.SubscribeApplicationRequest
import com.letsconfig.network.grpc.common.SubscriberInfoRequest
import com.letsconfig.network.grpc.common.UpdatePropertyRequest
import com.letsconfig.network.grpc.common.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.Assert
import org.junit.rules.ExternalResource
import java.util.*

const val TEST_APP_NAME = "test-app"
const val TEST_HOST = "srvd1"

class ServiceRule : ExternalResource() {

    private val configurationDao = InMemoryConfigurationDao(emptyMap())
    val updateDelayMs: Long = 1000
    private val propertiesWatchDispatcher = PropertiesWatchDispatcher(
            configurationDao, ConfigurationResolver(), ThreadScheduler(), updateDelayMs)
    private val grpcConfServer = GrpcConfigurationServer(
            GrpcConfigurationService(
                    ConfigurationService(configurationDao, propertiesWatchDispatcher)
            )
    )

    val blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    val asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub

    init {
        val channel: ManagedChannel = ManagedChannelBuilder.forAddress("localhost", 8080)
                .usePlaintext()
                .build()
        blockingClient = ConfigurationServiceGrpc.newBlockingStub(channel)
        asyncClient = ConfigurationServiceGrpc.newStub(channel)
    }

    override fun before() {
        propertiesWatchDispatcher.start()
        startServer()
    }

    private fun startServer() {
        grpcConfServer.start()
    }

    override fun after() {
        stopServer()
    }

    private fun stopServer() {
        grpcConfServer.stop()
    }

    fun createApplication(app: String) {
        val res = blockingClient.createApplication(ApplicationRequest.newBuilder()
                .setApplicationName(app)
                .build())
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, res.type)
    }

    fun updateProperty(appName: String, hostName: String, version: Long?, propertyName: String, propertyValue: String) {
        val res = blockingClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version ?: 0)
                .build())
        Assert.assertEquals(UpdatePropertyResponse.Type.OK, res.type)
    }

    fun deleteProperty(appName: String, hostName: String, propertyName: String) {
        val res: DeletePropertyResponse = blockingClient.deleteProperty(DeletePropertyRequest.newBuilder()
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .build()
        )
        Assert.assertEquals(res.type, DeletePropertyResponse.Type.OK)
    }

    fun subscribeTestApplication(subscriberId: String, lastKnownVersion: Long?): ApplicationSnapshotResponse {
        return blockingClient.subscribeApplication(SubscribeApplicationRequest.newBuilder()
                .setApplicationName(TEST_APP_NAME)
                .setDefaultApplicationName("my-app")
                .setHostName(TEST_HOST)
                .setSubscriberId(subscriberId)
                .setLastKnownVersion(lastKnownVersion ?: 0)
                .build())
    }

    fun watchChanges(subscriberId: String, queue: Queue<PropertyItem>) {
        asyncClient.watchChanges(SubscriberInfoRequest.newBuilder().setId(subscriberId).build(), QueueStreamObserver(queue))
    }
}