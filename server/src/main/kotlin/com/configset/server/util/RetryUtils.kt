package com.configset.server.util

import org.slf4j.LoggerFactory

private val log = LoggerFactory.getLogger("RetryUtils")

fun <T> retry(delayMs: Long = 2000, maxRetries: Int = 3, f: () -> T): T {
    while (true) {
        try {
            return f()
        } catch (e: Exception) {
            log.warn("Exception while calling function. Retry in $delayMs ms.", e)
        }
        Thread.sleep(delayMs)
    }
}