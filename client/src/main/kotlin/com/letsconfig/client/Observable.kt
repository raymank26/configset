package com.letsconfig.client

interface Observable<T> {
    fun onSubscribe(subscriber: Subscriber<T>)
}
