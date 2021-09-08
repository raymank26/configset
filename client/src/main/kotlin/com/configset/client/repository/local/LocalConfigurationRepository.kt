package com.configset.client.repository.local

import com.configset.client.ChangingObservable
import com.configset.client.PropertyItem
import com.configset.client.repository.ConfigApplication
import com.configset.client.repository.ConfigurationRepository
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

    override fun subscribeToProperties(appName: String): ConfigApplication {
        val appValue: MutableList<PropertyItem> = mutableListOf()
        for (propertyName in properties.stringPropertyNames()) {
            if (propertyName.startsWith(appName)) {
                val nameParts = propertyName.split(".", limit = 2)
                appValue.add(PropertyItem(appName, nameParts[1], 0L, properties.getProperty(propertyName)))
            }
        }
        return ConfigApplication(appName, appValue, ChangingObservable())
    }

    override fun stop() = Unit
}