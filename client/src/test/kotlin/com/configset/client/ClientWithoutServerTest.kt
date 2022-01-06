package com.configset.client

import com.configset.client.converter.Converters
import eu.rekawek.toxiproxy.model.ToxicDirection
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.testcontainers.containers.ToxiproxyContainer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ClientWithoutServerTest {

    @Rule
    @JvmField
    val clientRule = ClientRule(true)

    private lateinit var proxy: ToxiproxyContainer.ContainerProxy

    @Before
    fun setUp() {
        proxy = clientRule.proxy!!
    }

    @Test
    fun testWaitingUntilConnectionEstablished() {
        proxy.toxics().timeout("timeout", ToxicDirection.DOWNSTREAM, 1000)

        Thread.sleep(2000) // waiting for connection loss

        val expectedValue = "value"
        val propertyName = "configuration.property"

        val setCalled = AtomicBoolean(false)

        thread {
            clientRule.createApplication(APP_NAME)
            clientRule.createHost(HOST_NAME)
            clientRule.updateProperty(APP_NAME, HOST_NAME, null, propertyName, expectedValue)
            setCalled.set(true)
            proxy.toxics()["timeout"].remove()
        }

        val confProperty: ConfProperty<String?> = clientRule.defaultConfiguration.getConfProperty(propertyName,
            Converters.STRING)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValue
        }
        setCalled.get() shouldBeEqualTo true
    }
}