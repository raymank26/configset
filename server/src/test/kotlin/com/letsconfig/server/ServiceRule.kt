package com.letsconfig.server

import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.PropertiesChangesResponse
import com.letsconfig.sdk.proto.SubscribeApplicationRequest
import com.letsconfig.sdk.proto.SubscriberInfoRequest
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import com.letsconfig.server.db.memory.InMemoryConfigurationDao
import com.letsconfig.server.network.grpc.GrpcConfigurationServer
import com.letsconfig.server.network.grpc.GrpcConfigurationService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.Assert
import org.junit.rules.ExternalResource
import java.util.*

const val TEST_APP_NAME = "test-app"
const val TEST_HOST = "srvd1"

class ServiceRule : ExternalResource() {

    private val configurationDao = InMemoryConfigurationDao()
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
        val res = blockingClient.createHost(CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(TEST_HOST).build())
        Assert.assertEquals(CreateHostResponse.Type.OK, res.type)
    }

    override fun after() {
        stopServer()
    }

    private fun stopServer() {
        grpcConfServer.stop()
    }

    fun createApplication(app: String, requestId: String = createRequestId()) {
        val res = blockingClient.createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(app)
                .build())
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, res.type)
    }

    fun updateProperty(appName: String, hostName: String, version: Long?, propertyName: String, propertyValue: String) {
        val res = blockingClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
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
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .build()
        )
        Assert.assertEquals(res.type, DeletePropertyResponse.Type.OK)
    }

    fun subscribeTestApplication(subscriberId: String, lastKnownVersion: Long?): PropertiesChangesResponse {
        return blockingClient.subscribeApplication(SubscribeApplicationRequest.newBuilder()
                .setApplicationName(TEST_APP_NAME)
                .setDefaultApplicationName("my-app")
                .setHostName(TEST_HOST)
                .setSubscriberId(subscriberId)
                .setLastKnownVersion(lastKnownVersion ?: 0)
                .build())
    }

    fun watchChanges(subscriberId: String, queue: Queue<PropertiesChangesResponse>) {
        asyncClient.watchChanges(SubscriberInfoRequest.newBuilder().setId(subscriberId).build(), QueueStreamObserver(queue))
    }

    fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }
}