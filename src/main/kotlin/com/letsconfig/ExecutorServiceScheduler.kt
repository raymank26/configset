package com.letsconfig

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ExecutorServiceScheduler : Scheduler {

    private val executor = Executors.newScheduledThreadPool(1)

    override fun scheduleAt(delayMs: Long, func: () -> Unit) {
        executor.scheduleWithFixedDelay({ func.invoke() }, 0, delayMs, TimeUnit.MILLISECONDS)
    }
}