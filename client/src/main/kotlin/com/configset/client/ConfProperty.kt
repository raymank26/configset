package com.configset.client

interface ConfProperty<T> {
    fun getValue(): T
    fun subscribe(listener: (T) -> Unit): Subscription
}
