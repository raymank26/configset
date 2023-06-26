package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldBeEqualTo
import org.awaitility.Awaitility
import org.junit.jupiter.api.Test

class RewriteRepositoryTest {

    @Test
    fun propertyValueUpdated() {
        // given
        val rewriteRegistry = ConfigurationRegistryFactory.getLocalRewriteConfiguration(
            ConfigurationTransport.LocalClasspath("/configuration.properties")
        )
        val someAppConfiguration = rewriteRegistry.getConfiguration("someApp")

        val targetPriceProperty = someAppConfiguration.getConfProperty("targetPrice", Converters.LONG)

        println(targetPriceProperty.getValue())

        // when
        rewriteRegistry.updateProperty("someApp", "targetPrice", "6")

        // then
        println(targetPriceProperty.getValue())
    }

    @Test
    fun propertyListenersCalled() {
        // given
        val rewriteRegistry = ConfigurationRegistryFactory.getLocalRewriteConfiguration(
            ConfigurationTransport.LocalClasspath("/configuration.properties")
        )
        val someAppConfiguration = rewriteRegistry.getConfiguration("someApp")

        val targetPriceProperty = someAppConfiguration.getConfProperty("targetPrice", Converters.LONG)

        var subscriptionCalled = false
        var newValue: Long? = null
        targetPriceProperty.subscribe {
            subscriptionCalled = true
            newValue = it
        }

        println(targetPriceProperty.getValue())

        // when
        rewriteRegistry.updateProperty("someApp", "targetPrice", "6")

        // then
        Awaitility.await().untilAsserted { subscriptionCalled shouldBe true }
        newValue shouldBeEqualTo 6
    }
}