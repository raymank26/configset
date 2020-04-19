package com.letsconfig.client

interface ConfProperty<T> {
    fun getValue(): T
    fun subscribe(listener: ConfPropertyListener<T>)
}

interface ConfPropertyListener<T> {
    fun consume(value: T)
}