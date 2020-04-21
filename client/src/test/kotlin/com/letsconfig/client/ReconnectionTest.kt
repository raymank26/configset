package com.letsconfig.client

import com.letsconfig.client.converter.Converters
import com.letsconfig.client.metrics.Metrics
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.Network
import org.testcontainers.containers.ToxiproxyContainer


class ReconnectionTest {

    @Rule
    @JvmField
    val toxiproxy = ToxiproxyContainer()
            .withNetwork(Network.newNetwork())

    @Rule
    @JvmField
    val serverRule = ServerRule(toxiproxy)

    private lateinit var proxy: ToxiproxyContainer.ContainerProxy

    @Before
    fun setUp() {
        proxy = serverRule.proxy!!
    }

    @Test
    fun testDeletePropertyDuringConnectionLoss() {
        val propertyName = "configuration.property"
        var confUpdates = 0
        val confProperty: ConfProperty<String?> = serverRule.defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

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

        Thread.sleep(5000) // waiting for connection loss

        serverRule.deleteProperty(APP_NAME, HOST_NAME, propertyName)

        Thread.sleep(5000) // waiting more

        confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate // here we check that property wan't changed

        proxy.toxics()["timeout"].remove()

        Thread.sleep(5000) // waiting more

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
        Thread.sleep(1000)

        confUpdates shouldBeEqualTo 2
        serverRule.metrics.get(Metrics.SKIPPED_OBSOLETE_UPDATES) shouldBeEqualTo 0
    }
}