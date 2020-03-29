package com.letsconfig

interface Scheduler {
    fun scheduleWithFixedDelay(initialDelayMs: Long, delayMs: Long, action: () -> Unit)
}