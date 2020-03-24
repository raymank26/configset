package com.letsconfig

class SingleObservable<T>(private val onSubscribe: (() -> Unit)?) {
    private var callback: ((T) -> Unit)? = null

    fun subscribe(callback: (T) -> Unit) {
        require(this.callback == null) { "callback has already set" }
        this.callback = callback
        onSubscribe?.invoke()
    }

    fun handle(event: T) {
        if (callback != null) {
            callback!!.invoke(event)
        } else {
            throw IllegalArgumentException("No observable has been set")
        }
    }
}