package com.configset.sdk

import org.slf4j.LoggerFactory

private val LOG = LoggerFactory.getLogger("Retries")

fun <T> retry(delayMs: Long = 2000, maxRetries: Int = 3, f: () -> T): T {
    var currentRetries = maxRetries
    while (currentRetries > 0) {
        try {
            return f()
        } catch (e: Exception) {
            LOG.warn("Exception while calling function. Retry in $delayMs ms.", e)
            Thread.sleep(delayMs)
            currentRetries--
            continue
        }
    }
    error("Unable to execute call")
}
