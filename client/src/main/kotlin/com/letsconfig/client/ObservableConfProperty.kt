package com.letsconfig.client

import com.letsconfig.client.converter.Converter
import com.letsconfig.sdk.extension.createLoggerStatic
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.concurrent.thread

private val LOG = createLoggerStatic<ObservableConfProperty<*>>()

private typealias Listener<T> = (T) -> Unit

class ObservableConfProperty<T>(
        private val name: String,
        private val defaultValue: T,
        private val converter: Converter<T>,
        dynamicValue: DynamicValue<String?, String?>) : ConfProperty<T> {

    @Volatile
    private var currentValue: T
    private val listeners: MutableCollection<Listener<T>> = CopyOnWriteArrayList()

    init {
        currentValue = covertSafely(dynamicValue.initial)
        dynamicValue.observable.onSubscribe(object : Subscriber<String?> {
            override fun process(value: String?) {
                currentValue = covertSafely(value)
                thread(name = "property-$name-updater") {
                    fireListeners(currentValue)
                }
            }
        })
    }

    override fun getValue(): T {
        return currentValue
    }

    override fun subscribe(listener: (T) -> Unit) {
        listeners.add(listener)
    }

    private fun covertSafely(value: String?): T {
        return try {
            if (value == null) {
                return defaultValue
            } else {
                converter.convert(value) ?: return defaultValue
            }
        } catch (e: Exception) {
            LOG.warn("For propertyName = $name unable to convert value = $value")
            defaultValue
        }
    }

    fun fireListeners(value: T) {
        for (listener in listeners) {
            try {
                listener.invoke(value)
            } catch (e: java.lang.Exception) {
                LOG.warn("For propertyName = $name unable to call listener for value = $value", e)
            }
        }
    }
}