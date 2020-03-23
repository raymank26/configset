package com.letsconfig

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class PropertiesObservableImpl(
        private val propertiesRepository: PropertiesRepository,
        private val scheduler: Scheduler,
        private val updateDelayMs: Long

) : PropertiesObservable {

    private val observers: MutableSet<PropertiesObserver> = Collections.newSetFromMap(ConcurrentHashMap())
    private var lastKnownVersion: Long? = null

    fun start() {
        updateProperties()
        scheduler.scheduleAt(updateDelayMs) {
            updateProperties()
        }
    }

    override fun addObserver(observer: PropertiesObserver) {
        observers.add(observer)
        observer.handleChanges(propertiesRepository.getUpdatedProperties(observer.lastKnownVersion(), lastKnownVersion!!))
    }

    private fun updateProperties() {
        val updatedProperties = propertiesRepository.getUpdatedProperties()
        for (observer in observers) {
            observer.handleChanges(updatedProperties)
        }
        lastKnownVersion = updatedProperties.lastVersion
    }

    override fun removeObserver(observer: PropertiesObserver) {
        observers.remove(observer)
    }
}