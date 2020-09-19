package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.Rule
import org.junit.Test

class ClientTest {

    @Rule
    @JvmField
    val serverRule = ServerRule()

    @Test
    fun `test subscribe update, delete routine`() {
        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = serverRule.defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        confProperty.getValue() shouldBeEqualTo null

        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOST_NAME)

        val expectedValueAfterUpdate = "123"

        serverRule.updateProperty(APP_NAME, HOST_NAME, null, propertyName, expectedValueAfterUpdate)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        serverRule.deleteProperty(APP_NAME, HOST_NAME, propertyName, 1)

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
    }

    @Test
    fun testDeleteCallback() {
        serverRule.createApplication(APP_NAME)
        serverRule.createHost(HOST_NAME)

        val propertyName = "configuration.property"
        val confProperty: ConfProperty<String?> = serverRule.defaultConfiguration.getConfProperty(propertyName, Converters.STRING)

        var deleteCaught = false
        var called = 0

        confProperty.subscribe { value ->
            if (value == null) {
                deleteCaught = true
            }
            called++
            println("called with arg = $value")
        }

        serverRule.updateProperty(APP_NAME, HOST_NAME, null, propertyName, "123")

        Awaitility.await().untilAsserted { called shouldBe 1 }

        serverRule.deleteProperty(APP_NAME, HOST_NAME, propertyName, 1)

        Awaitility.await().untilAsserted { deleteCaught shouldBe true }
        Awaitility.await().untilAsserted { called shouldBe 2 }
    }
}