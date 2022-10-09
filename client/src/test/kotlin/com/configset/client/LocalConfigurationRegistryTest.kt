package com.configset.client

import com.configset.client.converter.Converters
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class LocalConfigurationRegistryTest {

    @Test
    fun testReadProperties() {
        val registry = ConfigurationRegistryFactory.getConfiguration(ConfigurationTransport.LocalClasspath("/configuration.properties"))

        val someAppConfiguration = registry.getConfiguration("someApp")
        someAppConfiguration.getConfProperty("billingUrl", Converters.STRING).getValue() shouldBeEqualTo "https://billingurl.com"
        someAppConfiguration.getConfProperty("targetPrice", Converters.STRING).getValue() shouldBeEqualTo "5"
        someAppConfiguration.getConfProperty("service.updateTime", Converters.STRING).getValue() shouldBeEqualTo "10ms"

        registry.getConfiguration("someOtherApp").getConfProperty("retriesCount", Converters.INTEGER).getValue() shouldBeEqualTo 3
    }
}