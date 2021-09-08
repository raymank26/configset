package com.configset.client

private val EMPTY_SUBSCRIPTION = object : Subscription {
    override fun unsubscribe() = Unit
}

class ConstantConfProperty<T>(private val value: T) : ConfProperty<T> {

    override fun getValue(): T {
        return value
    }

    override fun subscribe(listener: Subscriber<T>): Subscription {
        return EMPTY_SUBSCRIPTION
    }
}