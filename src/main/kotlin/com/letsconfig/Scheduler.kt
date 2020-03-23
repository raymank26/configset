package com.letsconfig

interface Scheduler {
    fun scheduleAt(delayMs: Long, func: () -> Unit)
}