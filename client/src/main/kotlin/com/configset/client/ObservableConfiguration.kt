package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.converter.Converters
import com.configset.client.repository.ConfigurationRepository

class ObservableConfiguration(
    private val configurationRegistry: ConfigurationRegistry,
    appName: String,
    configurationRepository: ConfigurationRepository,
) : Configuration {

    private val registry =
        ApplicationRegistry(configurationRepository.subscribeToProperties(appName)) { appName, propertyName ->
            getConfiguration(appName).getConfProperty(propertyName, Converters.STRING)
        }

    fun start() {
        registry.start()
    }

    override fun getConfiguration(appName: String): Configuration {
        return configurationRegistry.getConfiguration(appName)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        return registry.getConfProperty(name, converter)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        return registry.getConfProperty(name, converter, defaultValue)
    }
}