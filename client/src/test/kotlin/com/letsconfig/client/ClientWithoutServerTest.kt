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
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ClientWithoutServerTest() {

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
    fun testWaitingUntilConnectionEstablished() {
        proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000)

        Thread.sleep(2000) // waiting for connection loss

        val expectedValue = "value"
        val propertyName = "configuration.property"

        val setCalled = AtomicBoolean(false)

        thread {
            serverRule.createApplication(APP_NAME)
            serverRule.createHost(HOST_NAME)
            serverRule.updateProperty(APP_NAME, HOST_NAME, null, propertyName, expectedValue)
            setCalled.set(true)
            proxy.toxics()["timeout"].remove()
        }

        val confProperty: ConfProperty<String?> = serverRule.defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValue
        }
        setCalled.get() shouldBeEqualTo true
    }
}