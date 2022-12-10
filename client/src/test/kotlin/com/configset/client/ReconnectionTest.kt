package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test

class ReconnectionTest : BaseClientTest() {

    @Test
    fun `reconnection should happen when connected is dropped`() {
        // given
        val propertyName = "configuration.property"
        var confUpdates = 0
        val confProperty: ConfProperty<String?> = defaultConfiguration.getConfProperty(propertyName, Converters.STRING)
        val expectedValueAfterUpdate = "123"

        confProperty.subscribe {
            confUpdates++
        }

        // when
        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, expectedValueAfterUpdate)
        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate
        }

        clientUtil.dropConnection()

        confProperty.getValue() shouldBeEqualTo expectedValueAfterUpdate // here we check that property wasn't changed
        clientUtil.pushPropertyDeleted(APP_NAME, propertyName)

        // then
        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo null
        }
        confUpdates shouldBeEqualTo 2
    }
}
