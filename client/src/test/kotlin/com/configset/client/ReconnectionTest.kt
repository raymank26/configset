package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.metrics.MetricKey
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.ToxiproxyContainer


class ReconnectionTest {

    @Rule
    @JvmField
    val serverRule = ClientRule(true)

    private lateinit var proxy: ToxiproxyContainer.ContainerProxy

    @Volatile
    private var connectionEstablished: Boolean = false

    @Before
    fun setUp() {
        proxy = serverRule.proxy!!

        serverRule.subscribeOnMetric(MetricKey.ConnectionEstablished) {
            connectionEstablished = true
        }
    }

    @Test
    fun testDeletePropertyDuringConnectionLoss() {
        var skippedObsoleteKeys = 0
        serverRule.subscribeOnMetric(MetricKey.SkippedObsoleteUpdate) {
            skippedObsoleteKeys++
        }
        val propertyName = "configuration.property"
        var confUpdates = 0
        val confProperty: ConfProperty<String?> =
            serverRule.defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        confProperty.subscribe {
            confUpdates++
        }

        confProperty.getValue() shouldBeEqualTo null

        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOST_NAME)

        val expectedValueAfterUpdate = "123"

        serverRule.updateProperty(APP_NAME, HOST_NAME, null, propertyName, expectedValueAfterUpdate)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000)

        awaitReconnection()

        serverRule.deleteProperty(APP_NAME, HOST_NAME, propertyName, 1)

        confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate // here we check that property wan't changed

        proxy.toxics()["timeout"].remove()

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }

        confUpdates shouldBeEqualTo 2
        skippedObsoleteKeys shouldBeEqualTo 0
    }

    private fun awaitReconnection() {
        Awaitility.await().untilAsserted {
            connectionEstablished
        }
        connectionEstablished = false
    }
}