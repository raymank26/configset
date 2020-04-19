package com.letsconfig.client

import com.letsconfig.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.junit.Test

class LocalConfigurationRegistryTest {

    @Test
    fun testReadProperties() {
        val registry = ConfigurationRegistryFactory.getConfiguration(ConfigurationTransport.LocalClasspath("/configuration.properties"))

        val someAppConfiguration = registry.getConfiguration("someApp")
        someAppConfiguration.getConfProperty("billingUrl", Converters.STRING).getValue() shouldBeEqualTo "https://billingurl.com"
        someAppConfiguration.getConfProperty("targetPrice", Converters.STRING).getValue() shouldBeEqualTo "5"

        registry.getConfiguration("someOtherApp").getConfProperty("retriesCount", Converters.INTEGER).getValue() shouldBeEqualTo 3
    }
}