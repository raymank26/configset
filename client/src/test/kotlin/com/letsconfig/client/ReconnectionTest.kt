package com.letsconfig.client

import com.letsconfig.client.converter.Converters
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
    fun `test subscribe update, delete routine`() {
        val propertyName = "configuration.property"
        var confUpdates = 0
        val confProperty: ConfProperty<String?> = serverRule.configuration.getConfProperty(propertyName, Converters.STRING)

        confProperty.subscribe {
            confUpdates++
        }

        confProperty.getValue() shouldBeEqualTo null

        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOSTNAME)

        val expectedValueAfterUpdate = "123"

        serverRule.updateProperty(APP_NAME, HOSTNAME, null, propertyName, expectedValueAfterUpdate)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000)

        Thread.sleep(5000) // waiting for connection loss

        serverRule.deleteProperty(APP_NAME, HOSTNAME, propertyName)

        Thread.sleep(5000) // waiting more

        confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate // here we check that property wan't changed

        proxy.toxics()["timeout"].remove()

        Thread.sleep(5000) // waiting more

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }

        confUpdates shouldBeEqualTo 2
    }
}