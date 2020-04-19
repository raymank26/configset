package com.letsconfig.client

interface Subscriber<T> {
    fun process(value: T)
}