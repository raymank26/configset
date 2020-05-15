package com.letsconfig.server

import com.letsconfig.sdk.extension.createLogger
import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.PropertiesChangesResponse
import com.letsconfig.sdk.proto.PropertyItem
import com.letsconfig.sdk.proto.ReadPropertyRequest
import com.letsconfig.sdk.proto.SubscribeApplicationRequest
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import com.letsconfig.sdk.proto.UpdateReceived
import com.letsconfig.sdk.proto.WatchRequest
import com.letsconfig.server.db.memory.InMemoryConfigurationDao
import com.letsconfig.server.network.grpc.GrpcConfigurationServer
import com.letsconfig.server.network.grpc.GrpcConfigurationService
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.rules.ExternalResource
import java.util.*
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

const val TEST_APP_NAME = "test-app"
const val TEST_DEFAULT_APP_NAME = "my-app"
const val TEST_HOST = "srvd1"


class ServiceRule : ExternalResource() {

    private val log = createLogger()
    private val configurationDao = InMemoryConfigurationDao()
    val updateDelayMs: Long = 1000
    private val propertiesWatchDispatcher = PropertiesWatchDispatcher(
            configurationDao, ConfigurationResolver(), ThreadScheduler(), updateDelayMs)
    private val grpcConfServer = GrpcConfigurationServer(
            GrpcConfigurationService(
                    ConfigurationService(configurationDao, propertiesWatchDispatcher)
            )
    )
    private val changesQueue = LinkedBlockingDeque<PropertiesChangesResponse>()
    private lateinit var subscribeStream: StreamObserver<WatchRequest>

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
        blockingClient.createHost(CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(TEST_HOST).build()).type shouldBeEqualTo CreateHostResponse.Type.OK

        createHost("host-$TEST_DEFAULT_APP_NAME")

        subscribeStream = asyncClient.watchChanges(object : StreamObserver<PropertiesChangesResponse> {
            override fun onNext(value: PropertiesChangesResponse) {
                changesQueue.add(value)
            }

            override fun onError(t: Throwable) {
                log.warn("Error occurred in response stream", t)
            }

            override fun onCompleted() {
            }
        })
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

    fun createHost(hostName: String) {
        blockingClient.createHost(CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(hostName).build()).type shouldBeEqualTo CreateHostResponse.Type.OK
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

    fun deleteProperty(appName: String, hostName: String, propertyName: String, version: Long,
                       expectedType: DeletePropertyResponse.Type = DeletePropertyResponse.Type.OK) {

        val res: DeletePropertyResponse = blockingClient.deleteProperty(DeletePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setVersion(version)
                .build()
        )
        Assert.assertEquals(expectedType, res.type)
    }

    fun subscribeTestApplication(lastKnownVersion: Long?) {
        val data = WatchRequest.newBuilder()
                .setType(WatchRequest.Type.SUBSCRIBE_APPLICATION)
                .setSubscribeApplicationRequest(SubscribeApplicationRequest.newBuilder()
                        .setApplicationName(TEST_APP_NAME)
                        .setDefaultApplicationName(TEST_DEFAULT_APP_NAME)
                        .setHostName(TEST_HOST)
                        .setLastKnownVersion(lastKnownVersion ?: 0)
                        .build())
                .build()

        subscribeStream.onNext(data)
    }

    fun watchForChanges(count: Int, timeoutMs: Long): List<PropertiesChangesResponse> {
        val res = mutableListOf<PropertiesChangesResponse>()
        var consumed = 0
        while (consumed != count) {
            val value = changesQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
                    ?: throw AssertionError("No value in timeout")
            if (value.itemsCount != 0) {
                consumed++
                res.add(value)

                subscribeStream.onNext(WatchRequest.newBuilder()
                        .setType(WatchRequest.Type.UPDATE_RECEIVED)
                        .setUpdateReceived(UpdateReceived.newBuilder()
                                .setApplicationName(value.applicationName)
                                .setVersion(value.lastVersion))
                        .build()
                )
            }
        }
        return res
    }

    fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }

    fun readProperty(appName: String, hostName: String, propertyName: String): PropertyItem? {
        val response = blockingClient.readProperty(ReadPropertyRequest.newBuilder().setApplicationName(appName)
                .setHostName(hostName).setPropertyName(propertyName).build())
        return if (response.hasItem) {
            response.item
        } else {
            null
        }
    }
}