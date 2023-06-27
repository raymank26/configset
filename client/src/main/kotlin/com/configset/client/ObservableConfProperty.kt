package com.configset.client

import com.configset.client.converter.Converter
import com.configset.common.client.extension.createLoggerStatic

private val LOG = createLoggerStatic<ObservableConfProperty<*>>()

class ObservableConfProperty<T>(
    private val configPropertyLinkProcessor: ConfigPropertyLinkProcessor,
    private val valueDependencyResolver: PropertyFullResolver,
    private val name: String,
    private val defaultValue: T,
    private val converter: Converter<T>,
    configurationSnapshot: ConfigurationSnapshot,
) : ConfProperty<T> {

    @Volatile
    private lateinit var state: PropertyState<T>
    private val listeners: MutableSet<Subscriber<T>> = HashSet()

    init {
        evaluate(configurationSnapshot.get(name)?.value)
        val subscriber = Subscriber<String?> { value -> evaluate(value) }

        configurationSnapshot.subscribe(name, subscriber)
    }

    override fun getValue(): T {
        return state.value
    }

    @Synchronized
    private fun evaluate(value: String?) {
        if (::state.isInitialized) {
            state.dependencySubscriptions.map { it.unsubscribe() }
        }
        val depSubscriptions = mutableListOf<Subscription>()
        val currentValueStr = if (value != null) {
            val tokensNode = configPropertyLinkProcessor.parse(value)
            for (token in tokensNode.tokens) {
                if (token is Link) {
                    val sub = valueDependencyResolver.getConfProperty(token.appName, token.propertyName).subscribe {
                        evaluate(value)
                    }
                    depSubscriptions.add(sub)
                }
            }
            configPropertyLinkProcessor.evaluate(tokensNode, valueDependencyResolver)
        } else {
            null
        }
        state = PropertyState(convertSafely(currentValueStr), depSubscriptions)
        fireListeners(state.value)
    }

    @Synchronized
    override fun subscribe(listener: Subscriber<T>): Subscription {
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
            LOG.warn("For propertyName = $name unable to convert value = $value", e)
            defaultValue
        }
    }

    private fun fireListeners(value: T) {
        // it might be possible that somebody unsubscribes while iterating. That's why we do a copy here.
        for (listener in HashSet(listeners)) {
            try {
                LOG.debug("Listener of {}", this)
                listener.process(value)
            } catch (e: Exception) {
                LOG.warn("For propertyName = $name unable to call listener for value = $value", e)
            }
        }
    }
}

data class PropertyState<T>(val value: T, val dependencySubscriptions: List<Subscription>)
