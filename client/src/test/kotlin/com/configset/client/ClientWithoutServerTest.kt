package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class ClientWithoutServerTest : BaseClientTest() {

    @Test
    fun testWaitingUntilConnectionEstablished() {
        clientUtil.dropConnection()
        val expectedValue = "value"
        val propertyName = "configuration.property"

        val setCalled = AtomicBoolean(false)

        thread {
            Thread.sleep(1000)
            clientUtil.pushPropertyUpdate(APP_NAME, propertyName, expectedValue)
            setCalled.set(true)
        }

        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValue
        }
        setCalled.get() shouldBeEqualTo true
    }
}
