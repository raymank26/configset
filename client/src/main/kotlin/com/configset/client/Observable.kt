package com.configset.client

interface Observable<T> {
    fun onSubscribe(subscriber: Subscriber<T>)
    fun push(value: T)
}
