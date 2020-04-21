package com.letsconfig.client

import com.letsconfig.client.metrics.LibraryMetrics
import java.util.concurrent.ConcurrentHashMap

class TestMetrics : LibraryMetrics {

    private val metricsMap = ConcurrentHashMap<String, Int>()

    override fun increment(metricName: String) {
        metricsMap.compute(metricName) { _, v ->
            if (v == null) {
                1
            } else {
                v + 1
            }
        }
    }

    override fun get(metricName: String): Int {
        return metricsMap.getOrDefault(metricName, 0)
    }
}