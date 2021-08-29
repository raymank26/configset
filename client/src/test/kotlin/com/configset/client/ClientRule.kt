package com.configset.client

import com.configset.client.metrics.LibraryMetrics
import com.configset.test.fixtures.CrudServiceRule
import org.junit.rules.ExternalResource
import org.testcontainers.Testcontainers
import org.testcontainers.containers.ToxiproxyContainer
import java.util.*

const val APP_NAME = "test"
const val HOST_NAME = "srvd1"
const val DEFAULT_APP_NAME = "default-app"

private const val INTERNAL_PORT = 8080

class ClientRule(private val useProxy: Boolean = false) : ExternalResource() {

    private var toxiproxyContainer: ToxiproxyContainer? = null
    private val crudServiceRule = CrudServiceRule()

    val defaultConfiguration: Configuration by lazy {
        registry.getConfiguration(APP_NAME)
    }

    private lateinit var registry: ConfigurationRegistry
    lateinit var metrics: LibraryMetrics

    var proxy: ToxiproxyContainer.ContainerProxy? = null

    override fun before() {
        crudServiceRule.before()

        if (useProxy) {
            Testcontainers.exposeHostPorts(INTERNAL_PORT)
            toxiproxyContainer = ToxiproxyContainer()
            proxy = toxiproxyContainer!!.apply {
                start()
            }.getProxy("host.testcontainers.internal", INTERNAL_PORT)
        }
        val backendHost = proxy?.containerIpAddress ?: "localhost"

        val backendPort = proxy?.proxyPort ?: INTERNAL_PORT
        metrics = TestMetrics()
        registry = ConfigurationRegistryFactory.getConfiguration(ConfigurationTransport.RemoteGrpc(HOST_NAME,
            DEFAULT_APP_NAME, backendHost, backendPort, metrics))
    }

    override fun after() {
        toxiproxyContainer?.stop()
        registry.stop()
        crudServiceRule.after()
    }

    fun createApplication(app: String, requestId: String = createRequestId()) {
        crudServiceRule.createApplication(app, requestId)
    }

    fun createHost(hostName: String) {
        crudServiceRule.createHost(hostName)
    }

    fun updateProperty(appName: String, hostName: String, version: Long?, propertyName: String, propertyValue: String) {
        crudServiceRule.updateProperty(appName, hostName, version, propertyName, propertyValue)
    }

    fun deleteProperty(appName: String, hostName: String, propertyName: String, version: Long) {
        crudServiceRule.deleteProperty(appName, hostName, propertyName, version)
    }

    private fun createRequestId(): String {
        return UUID.randomUUID().toString()
    }
}
