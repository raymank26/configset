package com.configset.client.metrics

import com.configset.client.Observable
import com.configset.client.Subscriber

typealias LibraryMetrics = Observable<MetricKey>

sealed class MetricKey {
    object SkippedObsoleteUpdate : MetricKey()
    object ConnectionEstablished : MetricKey()
}

object NoopLibraryMetrics : LibraryMetrics {
    override fun onSubscribe(subscriber: Subscriber<MetricKey>) {
    }

    override fun push(value: MetricKey) {
    }
}