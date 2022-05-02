package com.configset.server.util

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RetryUtils")

fun <T> retry(delayMs: Long = 2000, maxRetries: Int = 3, f: () -> T): T {
    var currentRetries = maxRetries
    while (currentRetries > 0) {
        try {
            return f()
        } catch (e: Exception) {
            log.warn("Exception while calling function. Retry in $delayMs ms.", e)
            Thread.sleep(delayMs)
            currentRetries--
            continue
        }
    }
    error("Unable to execute call")
}