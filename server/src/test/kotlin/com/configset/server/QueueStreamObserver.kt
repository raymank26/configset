package com.configset.server

import io.grpc.stub.StreamObserver
import java.util.*

class QueueStreamObserver<T>(private val queue: Queue<T>) : StreamObserver<T> {

    override fun onNext(value: T) {
        queue.add(value)
    }

    override fun onError(t: Throwable?) {
        throw IllegalArgumentException(t)
    }

    override fun onCompleted() {
    }
}