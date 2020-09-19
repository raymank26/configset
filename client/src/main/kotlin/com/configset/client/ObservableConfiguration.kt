package com.configset.client

import com.configset.client.converter.Converter

class ObservableConfiguration(
        private val appName: String,
        private val configurationRegistry: ConfigurationRegistry
) : Configuration {

    override fun getConfiguration(appName: String): Configuration {
        return configurationRegistry.getConfiguration(appName)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        return configurationRegistry.getConfProperty(appName, name, converter)
    }

    override fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        return configurationRegistry.getConfProperty(appName, name, converter, defaultValue)
    }
}