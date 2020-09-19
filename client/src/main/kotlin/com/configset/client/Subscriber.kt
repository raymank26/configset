package com.configset.client

interface Subscriber<T> {
    fun process(value: T)
}