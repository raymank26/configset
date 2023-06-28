package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class ClientTest : BaseClientTest() {

    @Test
    fun `test subscribe update, delete routine`() {
        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        confProperty.getValue() shouldBeEqualTo null

        val expectedValueAfterUpdate = "123"

        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, expectedValueAfterUpdate)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        clientUtil.pushPropertyDeleted(APP_NAME, propertyName)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
    }

    @Test
    fun `test delete callback`() {
        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        val deleteCaught = AtomicBoolean()
        val called = AtomicInteger()

        confProperty.subscribe { value ->
            if (value == null) {
                deleteCaught.set(true)
            }
            called.incrementAndGet()
            println("called with arg = $value")
        }

        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, "123")

        Awaitility.await().untilAsserted { called.get() shouldBe 1 }

        clientUtil.pushPropertyDeleted(APP_NAME, propertyName)

        Awaitility.await().untilAsserted { deleteCaught.get() shouldBe true }
        Awaitility.await().untilAsserted { called.get() shouldBe 2 }
    }

    @Test
    fun shouldHandleNotNullProperty() {
        val propertyName = "configuration.property"
        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, "text")

        Awaitility.await()
            .ignoreExceptions()
            .untilAsserted {
                defaultConfiguration.getConfPropertyNotNull(propertyName, Converters.STRING)
                    .getValue() shouldBeEqualTo "text"
            }
    }

    @Test
    fun shouldThrowExceptionIfPropertyNotFound() {
        val propertyName = "configuration.property"
        Assertions.assertThrows(Exception::class.java) {
            defaultConfiguration.getConfPropertyNotNull(propertyName, Converters.STRING).getValue()
        }
    }

    @Test
    fun `test no application`() {
        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty("foo", Converters.STRING)
        confProperty.getValue() shouldBeEqualTo null
    }
}
