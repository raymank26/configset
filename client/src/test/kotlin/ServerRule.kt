import com.letsconfig.client.Configuration
import com.letsconfig.client.ConfigurationFactory
import com.letsconfig.client.ConfigurationTransport
import org.junit.rules.ExternalResource
import org.testcontainers.containers.GenericContainer

val APP_NAME = "test"
val HOSTNAME = "localhost"

private val INTERNAL_PORT = 8080

class ServerRule : ExternalResource() {

    private lateinit var container: KLetsconfigBackend

    lateinit var configuration: Configuration

    override fun before() {
        container = KLetsconfigBackend("letsconfig-backend:latest")
                .withExposedPorts(INTERNAL_PORT)
        container.start()

        configuration = ConfigurationFactory.getConfiguration(APP_NAME,
                ConfigurationTransport.RemoteGrpc(HOSTNAME, "localhost", container.getMappedPort(INTERNAL_PORT)))
    }

    override fun after() {
        container.stop()
    }
}

class KLetsconfigBackend(name: String) : GenericContainer<KLetsconfigBackend>(name)
