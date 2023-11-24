package com.configset.client

import com.configset.client.converter.Converters
import com.configset.client.reflect.PropertyInfo
import org.amshove.kluent.invoking
import org.amshove.kluent.shouldBeEqualTo
import org.amshove.kluent.shouldThrow
import org.amshove.kluent.withMessage
import org.junit.jupiter.api.Test
import java.net.URI

class LocalConfigurationRegistryTest {

    @Test
    fun testReadProperties() {
        val registry = ConfigurationRegistryFactory.getConfiguration(
            ConfigurationSource.File(
                URI("/configuration.properties"),
                FileLocation.CLASSPATH,
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
        // given
        val registry = ConfigurationRegistryFactory.getConfiguration(
            ConfigurationSource.File(
                URI("/configuration.toml"),
                FileLocation.CLASSPATH,
                FileFormat.TOML
            )
        )
        // when
        val configuration = registry.getConfiguration("web-app")

        // then
        configuration.getConfProperty("multiline", Converters.STRING).getValue() shouldBeEqualTo
                """some
                |multiline value
                |""".trimMargin("|")

        val obj = configuration.getConfPropertyInterface("subconfig", TomlInterface::class.java)
        obj.testKeyOne() shouldBeEqualTo "testValueOne"
        obj.testKeyTwo() shouldBeEqualTo "testValueTwo"
    }

    @Test
    fun testNestedMultilineException() {
        // then
        invoking {
            ConfigurationRegistryFactory.getConfiguration(
                ConfigurationSource.File(
                    URI("/configuration_nested_multiline.toml"),
                    FileLocation.CLASSPATH,
                    FileFormat.TOML
                )
            )
        } shouldThrow Exception::class withMessage "Nested multiline values are not supported"
    }

    @Test
    fun testImplicitConfig() {
        val configuration = ConfigurationRegistryFactory.getConfiguration(
            env = mapOf("CONFIG_URI" to "classpath:///configuration.toml?format=toml")
        ).getConfiguration("music-app")

        configuration.getConfPropertyNotNull("someKey", Converters.STRING).getValue() shouldBeEqualTo "someValue"
    }
}

private interface TomlInterface {

    @PropertyInfo
    fun testKeyOne(): String

    @PropertyInfo
    fun testKeyTwo(): String
}
