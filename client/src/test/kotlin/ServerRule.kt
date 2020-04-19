import com.letsconfig.client.Configuration
import com.letsconfig.client.ConfigurationFactory
import com.letsconfig.client.ConfigurationRegistry
import com.letsconfig.client.ConfigurationTransport
import org.junit.rules.ExternalResource
import org.testcontainers.containers.GenericContainer

val APP_NAME = "test"
val HOSTNAME = "localhost"

private val INTERNAL_PORT = 8080

class ServerRule : ExternalResource() {

    private lateinit var container: KLetsconfigBackend

    lateinit var configuration: Configuration
    lateinit var registry: ConfigurationRegistry

    override fun before() {
        container = KLetsconfigBackend("letsconfig-backend:latest")
                .withExposedPorts(INTERNAL_PORT)
        container.start()

        registry = ConfigurationFactory.getConfiguration(ConfigurationTransport.RemoteGrpc(HOSTNAME, "localhost", container.getMappedPort(INTERNAL_PORT)))
        configuration = registry.getConfiguration(APP_NAME)
    }

    override fun after() {
        registry.stop()
        container.stop()
    }
}

class KLetsconfigBackend(name: String) : GenericContainer<KLetsconfigBackend>(name)
