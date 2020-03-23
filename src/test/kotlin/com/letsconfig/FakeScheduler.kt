package com.letsconfig

class FakeScheduler : Scheduler {
    private val functions: MutableList<() -> Unit> = mutableListOf()

    override fun scheduleAt(delayMs: Long, func: () -> Unit) {
        functions.add(func)
    }

    fun fire() {
        for (function in functions) {
            function.invoke()
        }
    }
}