package com.letsconfig.sdk.extension

import org.slf4j.LoggerFactory

fun <T : Any> T.createLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(this.javaClass)
}