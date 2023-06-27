package com.configset.client

import com.configset.client.converter.Converter

class ApplicationRegistry(
    private val configurationSnapshot: ConfigurationSnapshot,
    private val propertyResolver: PropertyFullResolver,
) {

    private val inProgressResolution = mutableSetOf<String>()

    fun updateState(value: List<PropertyItem>) {
        configurationSnapshot.update(value)
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
        val res = ObservableConfProperty(
            configPropertyLinkProcessor = ConfigPropertyLinkProcessor.INSTANCE,
            valueDependencyResolver = propertyResolver,
            name = name,
            defaultValue = defaultValue,
            converter = converter,
            configurationSnapshot = configurationSnapshot,
        )
        inProgressResolution.remove(name)
        return res
    }
}

fun interface PropertyFullResolver {
    fun getConfProperty(appName: String, propertyName: String): ConfProperty<String?>
}
