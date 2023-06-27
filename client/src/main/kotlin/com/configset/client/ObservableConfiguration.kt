package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.converter.Converters
import com.configset.client.repository.ConfigurationRepository

class ObservableConfiguration<T : Configuration>(
    private val configurationRegistry: ConfigurationRegistry<T>,
    appName: String,
    configurationRepository: ConfigurationRepository,
) : UpdatableConfiguration {

    private val registry =
        ApplicationRegistry(configurationRepository.subscribeToProperties(appName)) { appName, propertyName ->
            getConfiguration(appName).getConfProperty(propertyName, Converters.STRING)
        }

    fun start() {
        registry.start()
    }

    override fun updateProperty(appName: String, name: String, value: String) {
        updatePropertyInternal(appName, name, value)
    }

    override fun deleteProperty(appName: String, name: String) {
        updatePropertyInternal(appName, name, null)
    }

    @Synchronized
    private fun updatePropertyInternal(
        appName: String,
        name: String,
        value: String?
    ) {
        registry.updateState(listOf(PropertyItem(appName, name, 1L, value)))
    }

    override fun getConfiguration(appName: String): T {
        return configurationRegistry.getConfiguration(appName)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        return registry.getConfProperty(name, converter)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        return registry.getConfProperty(name, converter, defaultValue)
    }
}
