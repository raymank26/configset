package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.reflect.PropertyInfo
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test

class LocalConfigurationRegistryTest {

    @Test
    fun testReadProperties() {
        val registry = ConfigurationRegistryFactory.getConfiguration(
            ConfigurationSource.File(
                "/configuration.properties",
                FileSourceType.CLASSPATH,
                FileFormat.PROPERTIES
            )
        )

        val someAppConfiguration = registry.getConfiguration("someApp")
        someAppConfiguration.getConfProperty("billingUrl", Converters.STRING)
            .getValue() shouldBeEqualTo "https://billingurl.com"
        someAppConfiguration.getConfProperty("targetPrice", Converters.STRING).getValue() shouldBeEqualTo "5"
        someAppConfiguration.getConfProperty("service.updateTime", Converters.STRING).getValue() shouldBeEqualTo "10ms"

        registry.getConfiguration("someOtherApp").getConfProperty("retriesCount", Converters.INTEGER)
            .getValue() shouldBeEqualTo 3
    }

    @Test
    fun testReadToml() {
        val registry = ConfigurationRegistryFactory.getConfiguration(
            ConfigurationSource.File(
                "/configuration.toml",
                FileSourceType.CLASSPATH,
                FileFormat.TOML
            )
        )
        val someAppConfiguration = registry.getConfiguration("web-app")
            .getConfPropertyInterface("multilineKey", TomlInterface::class.java)

        someAppConfiguration.testKeyOne() shouldBeEqualTo "testValueOne"
        someAppConfiguration.testKeyTwo() shouldBeEqualTo "testValueTwo"
    }
}

private interface TomlInterface {

    @PropertyInfo
    fun testKeyOne(): String

    @PropertyInfo
    fun testKeyTwo(): String
}
