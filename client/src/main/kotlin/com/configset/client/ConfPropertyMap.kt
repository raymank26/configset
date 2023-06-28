package com.configset.client

fun <T, U> ConfProperty<T>.map(mapping: (T) -> U): ConfProperty<U> {
    val that = this

    return object : ConfProperty<U> {

        override fun getValue(): U {
            return mapping(that.getValue())
        }

        override fun subscribe(listener: Subscriber<U>): Subscription {
            val innerSubscription = that.subscribe {
                listener.process(mapping(it))
            }
            return object : Subscription {
                override fun unsubscribe() {
                    innerSubscription.unsubscribe()
                }
            }
        }
    }
}
