package com.letsconfig

import com.google.common.util.concurrent.ThreadFactoryBuilder
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class ThreadScheduler : Scheduler {
    private val log = log()

    private val scheduler = Executors.newScheduledThreadPool(16, ThreadFactoryBuilder().setDaemon(true).build())

    override fun scheduleWithFixedDelay(initialDelayMs: Long, delayMs: Long, action: () -> Unit) {
        this.javaClass
        scheduler.scheduleWithFixedDelay({
            try {
                action.invoke()
            } catch (e: Exception) {
                log.info("Exception occurred", e)
            } catch (e: Throwable) {
                log.info("Exception occurred", e)
                throw e
            }
        }, initialDelayMs, delayMs, TimeUnit.MILLISECONDS)
    }
}