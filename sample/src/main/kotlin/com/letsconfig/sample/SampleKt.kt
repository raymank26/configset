package com.letsconfig.sample

import com.letsconfig.client.ConfProperty
import com.letsconfig.client.Configuration
import com.letsconfig.client.ConfigurationRegistry
import com.letsconfig.client.ConfigurationRegistryFactory
import com.letsconfig.client.ConfigurationTransport
import com.letsconfig.client.converter.Converters
import com.letsconfig.sdk.extension.createLoggerStatic
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

    val configuration: ConfigurationRegistry = ConfigurationRegistryFactory.getConfiguration(
            ConfigurationTransport.RemoteGrpc(hostname, BILLING_APP, backendHost, backendPort))

    val sampleAppConfiguration: Configuration = configuration.getConfiguration(BILLING_APP)

    val priceProperty: ConfProperty<Int> = sampleAppConfiguration.getConfProperty("app.price.usd", Converters.INTEGER, DEFAULT_PRICE)

    LOG.info("Initial price value = ${priceProperty.getValue()}")

    priceProperty.subscribe { newPrice ->
        LOG.info("New price = $newPrice")
    }

    semaphore.acquire()
}

