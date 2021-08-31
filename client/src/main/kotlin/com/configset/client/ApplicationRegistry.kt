package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.repository.ConfigApplication
import com.configset.sdk.extension.createLoggerStatic

private val LOG = createLoggerStatic<ApplicationRegistry>()

class ApplicationRegistry(private val propertiesProvider: ConfigApplication) {

    private val appName = propertiesProvider.appName
    private val propertiesSubscribers: MutableMap<String, ChangingObservable<String?>> = HashMap()
    private val snapshot: MutableMap<String, String> =
        propertiesProvider.initial.associate { it.name to it.value!! }.toMutableMap()

    fun start() {
        propertiesProvider.observable.onSubscribe(object : Subscriber<List<PropertyItem>> {
            override fun process(value: List<PropertyItem>) {
                updateState(value)
            }
        })
    }

    @Synchronized
    private fun updateState(value: List<PropertyItem>) {
        for (propertyItem in value) {
            LOG.info("Update come for appName = ${appName}, property = $propertyItem")
            propertiesSubscribers[propertyItem.name]?.setValue(propertyItem.value)
        }
        for (propertyItem in value) {
            if (propertyItem.value != null) {
                snapshot[propertyItem.name] = propertyItem.value
            }
        }
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        return getConfProperty(name, converter, null)
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        val value = snapshot[name]
        val observable = propertiesSubscribers.compute(name) { _, prev ->
            prev ?: ChangingObservable()
        }!!
        return ObservableConfProperty(name, value?.let { converter.convert(it) } ?: defaultValue, converter,
                DynamicValue(value, observable))
    }
}