package com.configset.client

class ConstantConfProperty<T>(private val value: T) : ConfProperty<T> {

    override fun getValue(): T {
        return value
    }

    override fun subscribe(listener: (T) -> Unit) {
    }
}