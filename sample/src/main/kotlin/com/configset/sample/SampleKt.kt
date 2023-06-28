package com.configset.sample

import com.configset.client.ConfProperty
import com.configset.client.Configuration
import com.configset.client.ConfigurationRegistry
import com.configset.client.ConfigurationRegistryFactory
import com.configset.client.ConfigurationSource
import com.configset.client.converter.Converters
import com.configset.common.client.extension.createLoggerStatic
import java.util.concurrent.Semaphore

private const val BILLING_APP = "billing"
private const val DEFAULT_PRICE = 10

object SampleKt

private val LOG = createLoggerStatic<SampleKt>()

fun main() {
    val semaphore = Semaphore(0)
    val hostname = System.getenv()["hostname"]!!
    val backendHost = System.getenv()["config_server.hostname"]!!
    val backendPort = System.getenv()["config_server.port"]!!.toInt()

    val configuration: ConfigurationRegistry<Configuration> = ConfigurationRegistryFactory.getConfiguration(
        ConfigurationSource.Grpc(hostname, BILLING_APP, backendHost, backendPort, 10000)
    )

    val sampleAppConfiguration: Configuration = configuration.getConfiguration(BILLING_APP)

    val priceProperty: ConfProperty<Int> = sampleAppConfiguration.getConfProperty(
        "app.price.usd",
        Converters.INTEGER,
        DEFAULT_PRICE
    )

    LOG.info("Initial price value = ${priceProperty.getValue()}")

    priceProperty.subscribe { newPrice ->
        LOG.info("New price = {}", newPrice)
    }

    semaphore.acquire()
}
