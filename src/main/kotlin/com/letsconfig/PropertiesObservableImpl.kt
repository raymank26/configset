package com.letsconfig

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class PropertiesObservableImpl(
        private val propertiesRepository: PropertiesRepository,
        private val scheduler: Scheduler,
        private val updateDelayMs: Long

) : PropertiesObservable {

    private val observers: MutableSet<PropertiesObserver> = mutableSetOf()
    private val observersLock = ReentrantLock()

    @Volatile
    private var lastKnownVersion: Long? = null

    fun start() {
        updateProperties()
        scheduler.scheduleWithDelay(updateDelayMs) {
            updateProperties()
        }
    }

    override fun addObserver(observer: PropertiesObserver) {
        observer.handleChanges(propertiesRepository.getUpdatedProperties(observer.lastKnownVersion()))
        observersLock.withLock {
            observers.add(observer)
        }
    }

    private fun updateProperties() {
        val updatedProperties = propertiesRepository.getUpdatedProperties(lastKnownVersion)
        if (updatedProperties.items.isEmpty()) {
            require(lastKnownVersion == updatedProperties.lastVersion)
            return;
        }
        observersLock.withLock {
            for (observer in observers) {
                observer.handleChanges(updatedProperties)
            }
        }
        lastKnownVersion = updatedProperties.lastVersion
    }

    override fun removeObserver(observer: PropertiesObserver) {
        observersLock.withLock {
            observers.remove(observer)
        }
    }
}