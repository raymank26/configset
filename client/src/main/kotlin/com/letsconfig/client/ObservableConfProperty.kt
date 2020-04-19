package com.letsconfig.client

import com.letsconfig.client.converter.Converter
import java.util.concurrent.CopyOnWriteArrayList

class ObservableConfProperty<T>(
        dynamicValue: DynamicValue<String?, String?>,
        private val defaultValue: T,
        private val converter: Converter<T>) : ConfProperty<T> {

    @Volatile
    private var currentValue: T
    private val listeners: MutableCollection<ConfPropertyListener<T>> = CopyOnWriteArrayList()

    init {
        currentValue = covertSafely(dynamicValue.initial)
        dynamicValue.observable.onSubscribe(object : Subscriber<String?> {
            override fun process(value: String?) {
                currentValue = covertSafely(value)
                fireListeners(currentValue)
            }
        })
    }

    override fun getValue(): T {
        return currentValue
    }

    override fun subscribe(listener: ConfPropertyListener<T>) {
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
            defaultValue
        }
    }

    @Synchronized
    fun fireListeners(value: T) {
        for (listener in listeners) {
            listener.consume(value)
        }
    }
}