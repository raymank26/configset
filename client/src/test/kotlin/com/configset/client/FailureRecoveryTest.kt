package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test

private const val DEFAULT_VALUE = 1L

class FailureRecoveryTest : BaseClientTest() {

    private val propertyName = "property.conf"
    private lateinit var confProperty: ConfProperty<Long>

    override fun setUp() {
        confProperty = defaultConfiguration.getConfProperty(propertyName, Converters.LONG, DEFAULT_VALUE)
    }

    @Test
    fun `test wrong value use default`() {
        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, "123")
        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo 123L
        }

        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, "10.23") // double is not long => default value

        Awaitility.await().untilAsserted {
            confProperty.getValue() shouldBeEqualTo DEFAULT_VALUE
        }
    }

    @Test
    fun `test failed listener`() {
        var capturedValue: Long? = null
        val stringValue = "1023"

        confProperty.subscribe { value ->
            capturedValue = value
            error("Failure for test only")
        }
        clientUtil.pushPropertyUpdate(APP_NAME, propertyName, stringValue)

        Awaitility.await().untilAsserted {
            capturedValue shouldBeEqualTo stringValue.toLong()
        }
    }
}
