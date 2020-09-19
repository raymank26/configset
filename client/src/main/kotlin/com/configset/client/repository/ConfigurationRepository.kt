package com.configset.client.repository

import com.configset.client.DynamicValue
import com.configset.client.PropertyItem

interface ConfigurationRepository {
    fun start()
    fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>>
    fun stop()
}