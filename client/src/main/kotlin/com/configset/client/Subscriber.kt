package com.configset.client

fun interface Subscriber<T> {
    fun process(value: T)
}
