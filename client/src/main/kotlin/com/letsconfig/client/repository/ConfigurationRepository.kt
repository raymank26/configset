package com.letsconfig.client.repository

import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem

interface ConfigurationRepository {
    fun start()
    fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>>
    fun stop()
}