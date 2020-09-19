package com.configset.client.metrics

interface LibraryMetrics {
    fun increment(metricName: String)
    fun get(metricName: String): Int
}

object NoopMetrics : LibraryMetrics {
    override fun increment(metricName: String) {
    }

    override fun get(metricName: String): Int {
        return 0
    }
}

object Metrics {
    const val SKIPPED_OBSOLETE_UPDATES = "SKIPPED_OBSOLETE_UPDATES"
}

