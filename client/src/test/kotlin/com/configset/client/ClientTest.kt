package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test

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

        var deleteCaught = false
        var called = 0

        confProperty.subscribe { value ->
            if (value == null) {
                deleteCaught = true
            }
            called++
            println("called with arg = $value")
        }

        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, "123")

        Awaitility.await().untilAsserted { called shouldBe 1 }

        clientUtil.pushPropertyDeleted(APP_NAME, propertyName)

        Awaitility.await().untilAsserted { deleteCaught shouldBe true }
        Awaitility.await().untilAsserted { called shouldBe 2 }
    }

    @Test
    fun `test no application`() {
        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty("foo", Converters.STRING)
        confProperty.getValue() shouldBeEqualTo null
    }
}