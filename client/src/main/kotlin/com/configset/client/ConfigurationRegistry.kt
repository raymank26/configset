package com.configset.client

import com.configset.client.repository.ConfigurationRepository
import java.util.concurrent.ConcurrentHashMap

private typealias AppName = String

class ConfigurationRegistry(
    private val configurationRepository: ConfigurationRepository,
) {

    private val appConfigs = ConcurrentHashMap<AppName, Configuration>()

    fun start() {
        configurationRepository.start()
    }

    fun getConfiguration(appName: String): Configuration {
        return appConfigs.getOrPut(appName) {
            val conf = ObservableConfiguration(this, appName, configurationRepository)
            conf.start()
            conf
        }
    }

    fun stop() {
        configurationRepository.stop()
    }
}
