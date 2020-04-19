package com.letsconfig.client

import java.util.concurrent.CopyOnWriteArrayList

class ChangingObservable<T> : Observable<T> {

    private val subscribers: MutableCollection<Subscriber<T>> = CopyOnWriteArrayList()

    override fun onSubscribe(subscriber: Subscriber<T>) {
        subscribers.add(subscriber)
    }

    fun setValue(value: T) {
        subscribers.forEach {
            it.process(value)
        }
    }
}