package com.configset.test.fixtures

import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
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
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Assert
import org.junit.rules.ExternalResource
import java.util.*
import java.util.concurrent.TimeUnit

const val TEST_APP_NAME = "test-app"
const val TEST_DEFAULT_APP_NAME = "my-app"
const val TEST_HOST = "srvd1"


class CrudServiceRule : ExternalResource() {

    private val serverStarterRule = ServiceStarterRule()

    val updateDelayMs: Long = 1000
    val blockingClient by lazy { serverStarterRule.blockingClient }
    val nonAuthBlockingClient by lazy { serverStarterRule.nonAuthBlockingClient }

    private val changesQueue by lazy { serverStarterRule.changesQueue }
    private val subscribeStream by lazy { serverStarterRule.subscribeStream }

    public override fun before() {
        serverStarterRule.before()
        blockingClient.createHost(CreateHostRequest.newBuilder()
            .setRequestId(createRequestId())
            .setHostName(TEST_HOST).build()).type shouldBeEqualTo CreateHostResponse.Type.OK
        createHost("host-$TEST_DEFAULT_APP_NAME")
    }

    public override fun after() {
        serverStarterRule.after()
    }

    fun createApplication(app: String, requestId: String = createRequestId()) {
        val meta = Metadata()
        meta.put(Metadata.Key.of("Authentication", Metadata.ASCII_STRING_MARSHALLER), "123")
        MetadataUtils.attachHeaders(blockingClient, meta)
        val res =
            MetadataUtils.attachHeaders(blockingClient, meta).createApplication(ApplicationCreateRequest.newBuilder()
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

    fun deleteProperty(
        appName: String, hostName: String, propertyName: String, version: Long,
        expectedType: DeletePropertyResponse.Type = DeletePropertyResponse.Type.OK,
    ) {

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