package com.letsconfig

interface ObservableQueue<T> {
    fun put(event: T)
    fun subscribe(onEvent: (T) -> Unit)
}