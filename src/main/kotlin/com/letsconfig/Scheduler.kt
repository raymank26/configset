package com.letsconfig

interface Scheduler {
    fun scheduleWithDelay(delayMs: Long, func: () -> Unit)
}