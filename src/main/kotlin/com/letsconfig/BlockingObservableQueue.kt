package com.letsconfig

import java.util.concurrent.ArrayBlockingQueue

class BlockingObservableQueue<T> : ObservableQueue<T> {
    private val singleObservable = SingleObservable<T>(this::startConsumer)
    private val blockingQueue = ArrayBlockingQueue<T>(100_000)

    private fun startConsumer() {
        Thread {
            while (true) {
                val event = blockingQueue.take()
                singleObservable.handle(event)
            }
        }.start()
    }

    override fun put(event: T) {
        blockingQueue.put(event)
    }

    override fun subscribe(onEvent: (T) -> Unit) {
        singleObservable.subscribe(onEvent)
    }
}