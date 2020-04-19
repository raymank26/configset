package com.letsconfig.client.repository

import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem

interface ConfigurationRepository {
    fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>>
}