package com.letsconfig.client

import com.letsconfig.client.converter.Converter
import com.letsconfig.client.repository.ConfigurationRepository
import java.util.concurrent.ConcurrentHashMap

private typealias AppName = String
private typealias PropertyName = String

class ConfigurationRegistry(
        private val configurationRepository: ConfigurationRepository
) {

    private val appStates: MutableMap<AppName, AppState> = ConcurrentHashMap()

    fun getConfiguration(appName: String): Configuration {
        return getApplicationRegistry(appName).configuration
    }

    fun <T> getConfProperty(appName: String, name: String, converter: Converter<T>): ConfProperty<T?> {
        return getApplicationRegistry(appName).applicationRegistry.getConfProperty(name, converter)
    }

    fun <T> getConfProperty(appName: String, name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        return getApplicationRegistry(appName).applicationRegistry.getConfProperty(name, converter, defaultValue)
    }

    private fun getApplicationRegistry(appName: String): AppState {
        return appStates.compute(appName) { _, appState ->
            if (appState != null) {
                appState
            } else {
                val registry = ApplicationRegistry(configurationRepository.subscribeToProperties(appName))
                registry.start()
                AppState(registry, ObservableConfiguration(appName, this))
            }
        }!!
    }
}

private data class AppState(val applicationRegistry: ApplicationRegistry, val configuration: Configuration)
