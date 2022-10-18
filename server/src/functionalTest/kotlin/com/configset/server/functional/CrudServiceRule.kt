package com.configset.server.functional

import com.configset.common.client.ConfigSetClient
import com.configset.common.client.extension.createLogger
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.CreateHostRequest
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.PropertiesChangesResponse
import com.configset.sdk.proto.PropertyItem
import com.configset.sdk.proto.ReadPropertyRequest
import com.configset.sdk.proto.SubscribeApplicationRequest
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import com.configset.sdk.proto.UpdateReceived
import com.configset.sdk.proto.WatchRequest
import com.configset.server.AppConfiguration
import com.configset.server.PropertiesWatchDispatcher
import com.configset.server.createAppModules
import com.configset.server.db.ConfigurationDao
import com.configset.server.db.memory.InMemoryConfigurationDao
import com.configset.server.fixtures.ACCESS_TOKEN
import com.configset.server.fixtures.SERVER_PORT
import com.configset.server.fixtures.TEST_APP_NAME
import com.configset.server.fixtures.TEST_DEFAULT_APP_NAME
import com.configset.server.fixtures.TEST_HOST
import com.configset.server.network.grpc.GrpcConfigurationServer
import io.grpc.stub.StreamObserver
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import java.util.UUID
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class CrudServiceRule : BeforeAllCallback, BeforeEachCallback, AfterEachCallback, AfterAllCallback {

    val updateDelayMs: Long = 1000
    val changesQueue = LinkedBlockingDeque<PropertiesChangesResponse>()

    lateinit var blockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    lateinit var nonAuthBlockingClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    lateinit var subscribeStream: StreamObserver<WatchRequest>
    lateinit var asyncClient: ConfigurationServiceGrpc.ConfigurationServiceStub

    private val log = createLogger()
    private lateinit var koinApp: KoinApplication
    private lateinit var inMemoryConfigurationDao: InMemoryConfigurationDao
    private lateinit var propertiesWatchDispatcher: PropertiesWatchDispatcher
    private val configSetClient: ConfigSetClient = ConfigSetClient("localhost", SERVER_PORT)

    override fun beforeAll(context: ExtensionContext?) {
        val mainModules = createAppModules(
            AppConfiguration(
                mapOf(
                    "db_type" to "memory",
                    "grpc_port" to "8080",

                    "authenticator_type" to "stub",
                    "auth_stub.admin_access_token" to ACCESS_TOKEN
                )
            )
        )
        koinApp = koinApplication {
            modules(mainModules)
        }
        koinApp.koin.get<GrpcConfigurationServer>().start()
        inMemoryConfigurationDao = koinApp.koin.get<ConfigurationDao>() as InMemoryConfigurationDao
        propertiesWatchDispatcher = koinApp.koin.get()

        blockingClient = configSetClient.getAuthBlockingClient(ACCESS_TOKEN)
        nonAuthBlockingClient = configSetClient.blockingClient
        asyncClient = configSetClient.asyncClient
    }

    override fun beforeEach(context: ExtensionContext?) {
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
        blockingClient.createHost(
            CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(TEST_HOST).build()
        ).type shouldBeEqualTo CreateHostResponse.Type.OK
        createHost("host-$TEST_DEFAULT_APP_NAME")
    }

    override fun afterEach(context: ExtensionContext?) {
        subscribeStream.onCompleted()
        propertiesWatchDispatcher.clear()
        inMemoryConfigurationDao.cleanup()
        changesQueue.clear()
    }

    override fun afterAll(context: ExtensionContext?) {
        configSetClient.stop()
        koinApp.close()
    }

    fun createApplication(app: String, requestId: String = createRequestId()) {
        val res = blockingClient.createApplication(
            ApplicationCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(app)
                .build()
        )
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, res.type)
    }

    fun createHost(hostName: String) {
        blockingClient.createHost(
            CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(hostName).build()
        ).type shouldBeEqualTo CreateHostResponse.Type.OK
    }

    fun updateProperty(
        appName: String,
        hostName: String,
        version: Long?,
        propertyName: String,
        propertyValue: String,
    ) {
        val res = blockingClient.updateProperty(
            UpdatePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version ?: 0)
                .build()
        )
        Assert.assertEquals(UpdatePropertyResponse.Type.OK, res.type)
    }

    fun deleteProperty(
        appName: String, hostName: String, propertyName: String, version: Long,
        expectedType: DeletePropertyResponse.Type = DeletePropertyResponse.Type.OK,
    ) {

        val res: DeletePropertyResponse = blockingClient.deleteProperty(
            DeletePropertyRequest.newBuilder()
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
            .setSubscribeApplicationRequest(
                SubscribeApplicationRequest.newBuilder()
                    .setApplicationName(TEST_APP_NAME)
                    .setDefaultApplicationName(TEST_DEFAULT_APP_NAME)
                    .setHostName(TEST_HOST)
                    .setLastKnownVersion(lastKnownVersion ?: 0)
                    .build()
            )
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

                subscribeStream.onNext(
                    WatchRequest.newBuilder()
                        .setType(WatchRequest.Type.UPDATE_RECEIVED)
                        .setUpdateReceived(
                            UpdateReceived.newBuilder()
                                .setApplicationName(value.applicationName)
                                .setVersion(value.lastVersion)
                        )
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
        val response = blockingClient.readProperty(
            ReadPropertyRequest.newBuilder()
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .build()
        )
        return if (response.hasItem) {
            response.item
        } else {
            null
        }
    }
}