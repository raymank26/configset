package com.configset.client

import com.configset.client.converter.Converter
import com.configset.sdk.extension.createLoggerStatic
import java.util.*

private val LOG = createLoggerStatic<ObservableConfProperty<*>>()

private typealias Listener<T> = (T) -> Unit

class ObservableConfProperty<T>(
    private val configPropertyLinkProcessor: ConfigPropertyLinkProcessor,
    private val valueDependencyResolver: ValueDependencyResolver,
    private val name: String,
    private val defaultValue: T,
    private val converter: Converter<T>,
    dynamicValue: DynamicValue<String?, String?>,
) : ConfProperty<T> {

    @Volatile
    private lateinit var state: PropertyState<T>
    private val listeners: MutableSet<Listener<T>> =
        Collections.synchronizedSet(Collections.newSetFromMap(IdentityHashMap()))

    init {
        evaluate(dynamicValue.value)
        val subscriber = object : Subscriber<String?> {
            override fun process(value: String?) {
                evaluate(value)
            }
        }
        dynamicValue.observable.onSubscribe(subscriber)
    }

    override fun getValue(): T {
        return state.value
    }

    private fun evaluate(value: String?) {
        if (::state.isInitialized) {
            state.dependencySubscriptions.map { it.unsubscribe() }
        }
        val depSubscriptions = mutableListOf<Subscription>()
        val currentValueStr = if (value != null) {
            val tokensNode = configPropertyLinkProcessor.parse(value)
            for (token in tokensNode.tokens) {
                if (token is Link) {
                    val sub = valueDependencyResolver.resolve(token.appName, token.propertyName).subscribe {
                        evaluate(value)
                    }
                    depSubscriptions.add(sub)
                }
            }
            configPropertyLinkProcessor.evaluate(tokensNode, valueDependencyResolver)
        } else {
            value
        }
        state = PropertyState(convertSafely(currentValueStr), depSubscriptions)
        fireListeners(state.value)
    }

    override fun subscribe(listener: (T) -> Unit): Subscription {
        listeners.add(listener)
        return object : Subscription {
            override fun unsubscribe() {
                listeners.remove(listener)
            }
        }
    }

    private fun convertSafely(value: String?): T {
        return try {
            if (value == null) {
                return defaultValue
            } else {
                converter.convert(value)
            }
        } catch (e: Exception) {
            LOG.warn("For propertyName = $name unable to convert value = $value")
            defaultValue
        }
    }

    private fun fireListeners(value: T) {
        for (listener in listeners) {
            try {
                listener.invoke(value)
            } catch (e: Exception) {
                LOG.warn("For propertyName = $name unable to call listener for value = $value", e)
            }
        }
    }
}

fun interface ValueDependencyResolver {
    fun resolve(appName: String, propertyName: String): ConfProperty<String?>
}

data class PropertyState<T>(val value: T, val dependencySubscriptions: List<Subscription>)