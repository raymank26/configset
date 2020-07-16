package com.letsconfig.client.repository.local

import com.letsconfig.client.ChangingObservable
import com.letsconfig.client.DynamicValue
import com.letsconfig.client.PropertyItem
import com.letsconfig.client.repository.ConfigurationRepository
import java.io.Reader
import java.util.*

class LocalConfigurationRepository(private val readerProvider: () -> Reader) : ConfigurationRepository {

    private lateinit var properties: Properties

    override fun start() {
        properties = Properties()
        readerProvider.invoke().use {
            properties.load(it)
        }
    }

    override fun subscribeToProperties(appName: String): DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>> {
        val appValue: MutableList<PropertyItem.Updated> = mutableListOf()
        for (propertyName in properties.stringPropertyNames()) {
            if (propertyName.startsWith(appName)) {
                val nameParts = propertyName.split(".", limit = 2)
                appValue.add(PropertyItem.Updated(appName, nameParts[1], 0L, properties.getProperty(propertyName)))
            }
        }
        return DynamicValue(appValue, ChangingObservable())
    }

    override fun stop() {
    }
}