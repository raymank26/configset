package com.configset.client

import com.configset.client.converter.Converter
import com.configset.client.repository.ConfigApplication
import com.configset.common.client.extension.createLoggerStatic

private val LOG = createLoggerStatic<ApplicationRegistry>()

class ApplicationRegistry(
    private val propertiesProvider: ConfigApplication,
    private val propertyResolver: PropertyFullResolver,
) {

    private val appName = propertiesProvider.appName
    private val snapshot: MutableMap<String, DynamicValue<String?>> =
        propertiesProvider.initial.associate {
            it.name to (DynamicValue<String?>(it.value!!, ChangingObservable()))
        }.toMutableMap()
    private val inProgressResolution = mutableSetOf<String>()

    fun start() {
        propertiesProvider.observable.onSubscribe { value -> updateState(value) }
    }

    @Synchronized
    private fun updateState(value: List<PropertyItem>) {
        for (propertyItem in value) {
            LOG.info("Update come for appName = ${appName}, property = $propertyItem")
            val dynValue = snapshot[propertyItem.name]
            if (dynValue != null) {
                dynValue.observable.push(propertyItem.value)
            } else {
                snapshot[propertyItem.name] = DynamicValue(propertyItem.value!!, ChangingObservable())
            }
        }
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>): ConfProperty<T?> {
        return getConfProperty(name, converter, null)
    }

    @Synchronized
    fun <T> getConfProperty(name: String, converter: Converter<T>, defaultValue: T): ConfProperty<T> {
        if (inProgressResolution.contains(name)) {
            error("Recursive resolution found")
        }
        inProgressResolution.add(name)
        val dynamicValue = snapshot.getOrPut(name) {
            DynamicValue(null, ChangingObservable())
        }
        val res = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = propertyResolver,
            name = name,
            defaultValue = defaultValue,
            converter = converter,
            dynamicValue = dynamicValue
        )
        inProgressResolution.remove(name)
        return res
    }
}

fun interface PropertyFullResolver {
    fun getConfProperty(appName: String, propertyName: String): ConfProperty<String?>
}