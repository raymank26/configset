package com.configset.common.client.extension

import org.slf4j.LoggerFactory

fun <T : Any> T.createLogger(): org.slf4j.Logger {
    return LoggerFactory.getLogger(this.javaClass)
}

inline fun <reified T> createLoggerStatic(): org.slf4j.Logger {
    return LoggerFactory.getLogger(T::class.java)
}
