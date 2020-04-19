import com.letsconfig.client.Configuration
import com.letsconfig.client.ConfigurationFactory
import com.letsconfig.client.ConfigurationRegistry
import com.letsconfig.client.ConfigurationTransport
import com.letsconfig.sdk.extension.createLogger
import com.letsconfig.sdk.proto.ApplicationCreateRequest
import com.letsconfig.sdk.proto.ApplicationCreatedResponse
import com.letsconfig.sdk.proto.ConfigurationServiceGrpc
import com.letsconfig.sdk.proto.CreateHostRequest
import com.letsconfig.sdk.proto.CreateHostResponse
import com.letsconfig.sdk.proto.DeletePropertyRequest
import com.letsconfig.sdk.proto.DeletePropertyResponse
import com.letsconfig.sdk.proto.UpdatePropertyRequest
import com.letsconfig.sdk.proto.UpdatePropertyResponse
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import org.junit.Assert
import org.junit.rules.ExternalResource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import java.util.*
import java.util.concurrent.TimeUnit

const val APP_NAME = "test"
const val HOSTNAME = "srvd1"

private const val INTERNAL_PORT = 8080

class ServerRule : ExternalResource() {

    private lateinit var container: KLetsconfigBackend

    lateinit var configuration: Configuration
    lateinit var registry: ConfigurationRegistry
    private lateinit var crudChannel: ManagedChannel
    private lateinit var crudClient: ConfigurationServiceGrpc.ConfigurationServiceBlockingStub
    private val log = createLogger()

    override fun before() {
        container = KLetsconfigBackend("letsconfig-backend:latest")
                .withExposedPorts(INTERNAL_PORT)
        val logConsumer = Slf4jLogConsumer(log)
        container.start()
        container.followOutput(logConsumer)

        registry = ConfigurationFactory.getConfiguration(ConfigurationTransport.RemoteGrpc(HOSTNAME, "localhost", container.getMappedPort(INTERNAL_PORT)))
        configuration = registry.getConfiguration(APP_NAME)

        crudChannel = ManagedChannelBuilder.forAddress("localhost", container.getMappedPort(INTERNAL_PORT))
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

    fun deleteProperty(appName: String, hostName: String, propertyName: String) {
        val res: DeletePropertyResponse = crudClient.deleteProperty(DeletePropertyRequest.newBuilder()
                .setRequestId(createRequestId())
                .setApplicationName(appName)
                .setHostName(hostName)
                .setPropertyName(propertyName)
                .build()
        )
        Assert.assertEquals(res.type, DeletePropertyResponse.Type.OK)
    }

    private fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }
}

class KLetsconfigBackend(name: String) : GenericContainer<KLetsconfigBackend>(name)
