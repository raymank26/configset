package com.letsconfig.server

interface Scheduler {
    fun scheduleWithFixedDelay(initialDelayMs: Long, delayMs: Long, action: () -> Unit)
}