package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.converter.Converters
import com.configset.client.repository.ConfigurationRepository
import java.util.concurrent.ConcurrentHashMap

private typealias AppName = String

class ConfigurationRegistry(
    private val configurationRepository: ConfigurationRepository,
) {

    private val appStates: MutableMap<AppName, AppState> = ConcurrentHashMap()

    fun start() {
        configurationRepository.start()
    }

    fun getConfiguration(appName: String): Configuration {
        return getApplicationRegistry(appName).configuration
    }

    fun <T> getConfProperty(appName: String, name: String, converter: Converter<T>): ConfProperty<T?> {
        return getApplicationRegistry(appName).applicationRegistry.getConfProperty(name, converter)
    }

    fun <T> getConfProperty(appName: String, name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        val applicationRegistry = getApplicationRegistry(appName)
        return applicationRegistry.applicationRegistry.getConfProperty(name, converter, defaultValue)
    }

    fun stop() {
        configurationRepository.stop()
    }

    private fun getApplicationRegistry(appName: String): AppState {
        return appStates.compute(appName) { _, appState ->
            if (appState != null) {
                appState
            } else {
                val config = ObservableConfiguration(appName, this)
                val registry =
                    ApplicationRegistry(configurationRepository.subscribeToProperties(appName)) { appName, propertyName ->
                        getConfiguration(appName).getConfProperty(propertyName, Converters.STRING)
                    }
                registry.start()
                AppState(registry, config)
            }
        }!!
    }
}

private data class AppState(val applicationRegistry: ApplicationRegistry, val configuration: Configuration)
