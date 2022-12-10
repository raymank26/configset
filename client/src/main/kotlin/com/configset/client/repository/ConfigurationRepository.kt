package com.configset.client.repository

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem

interface ConfigurationRepository {
    fun start()
    fun subscribeToProperties(appName: String): ConfigApplication
    fun stop()
}

data class ConfigApplication(
    val appName: String,
    val initial: List<PropertyItem>,
    val observable: ChangingObservable<List<PropertyItem>>,
)
