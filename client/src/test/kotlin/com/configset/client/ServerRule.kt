package com.configset.client

import com.configset.client.metrics.LibraryMetrics
import com.configset.sdk.extension.createLogger
import com.configset.sdk.proto.ApplicationCreateRequest
import com.configset.sdk.proto.ApplicationCreatedResponse
import com.configset.sdk.proto.ConfigurationServiceGrpc
import com.configset.sdk.proto.CreateHostRequest
import com.configset.sdk.proto.CreateHostResponse
import com.configset.sdk.proto.DeletePropertyRequest
import com.configset.sdk.proto.DeletePropertyResponse
import com.configset.sdk.proto.UpdatePropertyRequest
import com.configset.sdk.proto.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.Assert
import org.junit.rules.ExternalResource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.ToxiproxyContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*
import java.util.concurrent.TimeUnit

const val APP_NAME = "test"
const val HOST_NAME = "srvd1"
const val DEFAULT_APP_NAME = "default-app"

private const val INTERNAL_PORT = 8080

class ServerRule(private val toxiproxyContainer: ToxiproxyContainer? = null) : ExternalResource() {

    private lateinit var container: KconfigsetBackend

    val defaultConfiguration: Configuration by lazy {
        registry.getConfiguration(APP_NAME)
    }

    lateinit var registry: ConfigurationRegistry
    lateinit var metrics: LibraryMetrics

    private lateinit var crudChannel: ManagedChannel
    private lateinit var crudClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    var proxy: ToxiproxyContainer.ContainerProxy? = null
    private val log = createLogger()

    override fun before() {
        container = KconfigsetBackend("configset-backend:latest")
                .withExposedPorts(INTERNAL_PORT)
        if (toxiproxyContainer != null) {
            container.withNetwork(toxiproxyContainer.network)
        }
        val logConsumer = Slf4jLogConsumer(log)
        container.start()
        container.followOutput(logConsumer)

        proxy = toxiproxyContainer?.getProxy(container, INTERNAL_PORT)
        val backendHost = proxy?.containerIpAddress ?: "localhost"
        val mappedPort = container.getMappedPort(INTERNAL_PORT)

        val backendPort = proxy?.proxyPort ?: mappedPort
        metrics = TestMetrics()
        registry = ConfigurationRegistryFactory.getConfiguration(ConfigurationTransport.RemoteGrpc(HOST_NAME,
                DEFAULT_APP_NAME, backendHost, backendPort, metrics))

        crudChannel = ManagedChannelBuilder.forAddress("localhost", mappedPort)
                .usePlaintext()
                .build()
        crudClient = ConfigurationServiceGrpc.newBlockingStub(crudChannel)
    }

    override fun after() {
        registry.stop()
        crudChannel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS)
        container.stop()
    }

    fun createApplication(app: String, requestId: String = createRequestId()) {
        val res = crudClient.createApplication(ApplicationCreateRequest.newBuilder()
                .setRequestId(requestId)
                .setApplicationName(app)
                .build())
        Assert.assertEquals(ApplicationCreatedResponse.Type.OK, res.type)
    }

    fun createHost(hostName: String) {
        val res = crudClient.createHost(CreateHostRequest.newBuilder()
                .setRequestId(createRequestId())
                .setHostName(hostName).build())
        Assert.assertEquals(CreateHostResponse.Type.OK, res.type)
    }

    fun updateProperty(appName: String, hostName: String, version: Long?, propertyName: String, propertyValue: String) {
        val res = crudClient.updateProperty(UpdatePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setPropertyValue(propertyValue)
                .setVersion(version ?: 0)
                .build())
        Assert.assertEquals(UpdatePropertyResponse.Type.OK, res.type)
    }

    fun deleteProperty(appName: String, hostName: String, propertyName: String, version: Long) {
        val res: DeletePropertyResponse = crudClient.deleteProperty(DeletePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .setVersion(version)
                .build()
        )
        Assert.assertEquals(res.type, DeletePropertyResponse.Type.OK)
    }

    private fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }
}

private class KconfigsetBackend(name: String) : GenericContainer<KconfigsetBackend>(name)
