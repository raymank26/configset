package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.repository.ConfigApplication
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.junit.Before
import org.junit.Test

class ApplicationRegistryTest {

    private lateinit var applicationRegistry: ApplicationRegistry

    @Before
    fun setup() {
        val initial = listOf(
            PropertyItem("some-app", "good.property", 1, "good value"),
            PropertyItem("some-app", "recursive.property", 1, "point to \${some-app\\recursive.property}"),
        )
        applicationRegistry = ApplicationRegistry(ConfigApplication("some-app", initial, ChangingObservable())
        ) { _, propertyName -> applicationRegistry.getConfProperty(propertyName, Converters.STRING) }
    }

    @Test
    fun testResolutionSuccess() {
        applicationRegistry.getConfProperty("good.property", Converters.STRING).getValue() shouldBeEqualTo "good value"
    }

    @Test
    fun testRecursiveAccess() {
        invoking {
            applicationRegistry.getConfProperty("recursive.property", Converters.STRING)
        } shouldThrow (Exception::class)
    }
}