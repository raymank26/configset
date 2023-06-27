package com.configset.client

import com.configset.client.repository.ConfigurationRepository
import java.util.concurrent.ConcurrentHashMap

private typealias AppName = String

open class ConfigurationRegistry<T : Configuration>(
    private val configurationRepository: ConfigurationRepository,
) {

    private val appConfigs = ConcurrentHashMap<AppName, T>()

    fun start() {
        configurationRepository.start()
    }

    fun getConfiguration(appName: String): T {
        val a: T = appConfigs.getOrPut<String, T>(appName) {
            val conf: ObservableConfiguration<T> = ObservableConfiguration(this, appName, configurationRepository)
            conf.start()
            (conf as T)
        }
        return a
    }

    fun stop() {
        configurationRepository.stop()
    }
}
