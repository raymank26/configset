package com.letsconfig.client

import com.letsconfig.client.converter.Converter

class ApplicationRegistry(
        private val propertiesProvider: DynamicValue<List<PropertyItem.Updated>, List<PropertyItem>>
) {

    private val propertiesSubscribers: MutableMap<String, ChangingObservable<String?>> = HashMap()
    private val snapshot: MutableMap<String, String>

    init {
        snapshot = propertiesProvider.initial.map { it.name to it.value }.toMap().toMutableMap()
    }

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
            val observable = propertiesSubscribers[propertyItem.name]
            if (observable != null) {
                when (propertyItem) {
                    is PropertyItem.Updated -> observable.setValue(propertyItem.value)
                    is PropertyItem.Deleted -> observable.setValue(null)
                }
            }
        }
        for (propertyItem in value) {
            if (propertyItem is PropertyItem.Updated) {
                snapshot[propertyItem.name] = propertyItem.value
            }
        }
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        val value = snapshot[name]
        val observable = propertiesSubscribers.compute(name) { _, prev ->
            prev ?: ChangingObservable()
        }!!
        return ObservableConfProperty(DynamicValue(value, observable), value?.let { converter.convert(it) }, converter)
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        val value = snapshot[name]
        val observable = propertiesSubscribers.compute(name) { _, prev ->
            prev ?: ChangingObservable()
        }!!
        return ObservableConfProperty(DynamicValue(value, observable), value?.let { converter.convert(it) }
                ?: defaultValue, converter)
    }
}