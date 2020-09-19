package com.configset.server

interface Scheduler {
    fun scheduleWithFixedDelay(initialDelayMs: Long, delayMs: Long, action: () -> Unit)
}