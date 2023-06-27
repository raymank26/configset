package com.configset.client.repository

import com.configset.client.ConfigurationSnapshot

interface ConfigurationRepository {
    fun start()
    fun subscribeToProperties(appName: String): ConfigurationSnapshot
    fun stop()
}
