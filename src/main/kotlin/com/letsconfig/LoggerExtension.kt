package com.letsconfig

import org.slf4j.LoggerFactory

fun <T : Any> T.log(): org.slf4j.Logger {
    return LoggerFactory.getLogger(this.javaClass)
}